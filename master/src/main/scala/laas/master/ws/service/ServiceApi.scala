/*
 * Copyright (c) 2023 Matteo Castellucci
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.cakelier
package laas.master.ws.service

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.DispatcherSelector
import akka.actor.typed.PostStop
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

import AnyOps.*
import laas.master.model.Executable.ExecutableId
import laas.master.model.Execution.ExecutionOutput
import laas.master.model.Performative
import laas.master.model.User.DeployedExecutable
import laas.master.ws.presentation.{Request, Response}
import laas.tuplespace.*
import laas.tuplespace.client.JsonTupleSpace

/** The actor representing the API of the service, managing all requests coming from the web app. */
private[ws] object ServiceApi {

  /* Deletes the executable received from the web app and then it sends to it a message signalling the failure of the deployment. */
  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  private def deleteExecutableAndComplete(
    executableId: ExecutableId,
    message: String,
    replyTo: ActorRef[Response]
  )(
    using
    ExecutionContext
  ): Unit =
    Future(Files.delete(Paths.get(executableId.toString)))
      .onComplete(_ => replyTo ! Response.DeployOutput(Failure(Exception(message))))

      /* The main behavior of this actor. */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion", "org.wartremover.warts.ToString"))
  private def main(
    ctx: ActorContext[ServiceApiCommand],
    storage: ServiceStorage,
    jsonTupleSpace: JsonTupleSpace,
    users: Map[UUID, String],
    sessions: Map[UUID, ActorRef[Response]]
  ): Behavior[ServiceApiCommand] = {
    given ExecutionContext = ctx.system.dispatchers.lookup(DispatcherSelector.blocking())
    Behaviors
      .receiveMessage[ServiceApiCommand] {
        case ServiceApiCommand.RequestCommand(request, id) =>
          sessions
            .get(id)
            .map(replyTo =>
              request match {
                case Request.Login(username, password) =>
                  (for {
                    s <- storage.login(username, password)
                    e <-
                      if (s)
                        storage.findByUsername(username)
                      else
                        Future.failed[Seq[DeployedExecutable]](Exception("Username or password are incorrect."))
                  } yield e).onComplete(t => ctx.self ! ServiceApiCommand.UserStateResponseCommand(username, id, t, replyTo))
                  Behaviors.same
                case Request.Logout =>
                  Behaviors.withTimers[ServiceApiCommand](s => {
                    s.cancel(id)
                    main(ctx, storage, jsonTupleSpace, users - id, sessions)
                  })
                case Request.Register(username, password) =>
                  storage
                    .register(username, password)
                    .onComplete(t =>
                      ctx.self ! ServiceApiCommand.UserStateResponseCommand(username, id, t.map(_ => Seq.empty), replyTo)
                    )
                  Behaviors.same
                case Request.UserState(oldId) =>
                  users
                    .get(oldId)
                    .fold {
                      replyTo ! Response.UserStateOutput(Failure(Exception("The session has expired, please log in again.")))
                      Behaviors.same
                    } { user =>
                      storage.findByUsername(user).onComplete(r => replyTo ! Response.UserStateOutput(r))
                      main(ctx, storage, jsonTupleSpace, users - oldId + (id -> user), sessions)
                    }
                case Request.Execute(executableId, args) =>
                  val executionId = UUID.randomUUID()
                  (for {
                    u <-
                      users
                        .get(id)
                        .fold(Future.failed[String](Exception("You need to be logged in to perform this operation.")))(
                          Future.successful
                        )
                    s <- storage.isExecutableOfUser(u, executableId)
                    _ <-
                      if (s)
                        jsonTupleSpace.out(
                          Performative.Request.name #:
                          "execute" #:
                          executionId.toString #:
                          executableId.toString #:
                          String.join(";", args: _*) #:
                          JsonNil
                        )
                      else
                        Future.failed[Unit](Exception("The executable id provided was not found."))
                    t <- jsonTupleSpace.in(
                      partial(
                        string in (Performative.InformResult.name, Performative.Failure.name),
                        "execute",
                        executionId.toString
                      )
                    )
                    o = t match {
                      case Performative.InformResult.name #: _ #: _ #: (e: Int) #: (out: String) #: (err: String) #: JsonNil =>
                        Success(ExecutionOutput(e, out, err))
                      case Performative.Failure.name #: _ #: _ #: (m: String) #: JsonNil => Failure[ExecutionOutput](Exception(m))
                      case _ => Failure[ExecutionOutput](Exception("Internal error."))
                    }
                  } yield o).onComplete(t => replyTo ! Response.ExecuteOutput(executableId, t.flatten))
                  Behaviors.same
              }
            )
            .getOrElse(Behaviors.same)
        case ServiceApiCommand.Open(actorRef, id) =>
          actorRef ! Response.SendId(id)
          main(ctx, storage, jsonTupleSpace, users, sessions + (id -> actorRef))
        case ServiceApiCommand.UserStateResponseCommand(username, id, result, replyTo) =>
          replyTo ! Response.UserStateOutput(result)
          if (result.isSuccess) {
            Behaviors.withTimers(s => {
              s.cancel(id)
              s.startSingleTimer(id, ServiceApiCommand.RequestCommand(Request.Logout, id), 1.day)
              main(ctx, storage, jsonTupleSpace, users + (id -> username), sessions)
            })
          } else {
            Behaviors.same
          }
        case ServiceApiCommand.Deploy(executableId, tpe, fileName, id) =>
          sessions
            .get(id)
            .foreach(replyTo =>
              users
                .get(id)
                .fold(
                  deleteExecutableAndComplete(executableId, "You need to be logged in to perform this operation.", replyTo)
                )(username => {
                  val cfpId = UUID.randomUUID()
                  jsonTupleSpace
                    .out(
                      Performative.Cfp.name #:
                      cfpId.toString #:
                      tpe.toString #:
                      JsonNil
                    )
                    .onComplete {
                      case Success(_) =>
                        ctx.self ! ServiceApiCommand.StartTimer(cfpId, tpe, executableId, username, fileName, replyTo)
                      case Failure(_) => deleteExecutableAndComplete(executableId, "The executable cannot be allocated.", replyTo)
                    }
                })
            )
          Behaviors.same
        case ServiceApiCommand.StartTimer(cfpId, tpe, executableId, username, fileName, replyTo) =>
          Behaviors.withTimers(s =>
            s.startSingleTimer(ServiceApiCommand.GetProposals(cfpId, tpe, executableId, username, fileName, replyTo), 5.seconds)
            Behaviors.same
          )
        case ServiceApiCommand.GetProposals(cfpId, tpe, executableId, username, fileName, replyTo) =>
          val strCfpId = cfpId.toString
          (for {
            _ <- jsonTupleSpace.in(
              complete(
                Performative.Cfp.name,
                cfpId.toString,
                tpe.toString
              )
            )
            a <- jsonTupleSpace.inAll(
              partial(
                string in (Performative.Propose.name, Performative.Refuse.name),
                strCfpId
              )
            )
            p = a.flatMap {
              case Performative.Propose.name #: strCfpId #: (workerId: String) #: (slotsAvailable: Int) #: JsonNil
                   if Try(UUID.fromString(workerId)).isSuccess =>
                Some(UUID.fromString(workerId) -> slotsAvailable)
              case _ => None
            }
          } yield p).onComplete {
            case Success(v) => ctx.self ! ServiceApiCommand.TryProposals(cfpId, executableId, tpe, v, username, fileName, replyTo)
            case Failure(_) => deleteExecutableAndComplete(executableId, "The executable cannot be allocated.", replyTo)
          }
          Behaviors.same
        case ServiceApiCommand.TryProposals(cfpId, executableId, tpe, proposals, username, fileName, replyTo) =>
          val strCfpId = cfpId.toString
          val bestProposal = proposals.maxByOption(_._2).map(_._1)
          bestProposal.fold(
            deleteExecutableAndComplete(executableId, "The executable cannot be allocated.", replyTo)
          )(b =>
            (for {
              _ <- jsonTupleSpace.out(
                Performative.AcceptProposal.name #:
                cfpId.toString #:
                b.toString #:
                executableId.toString #:
                tpe.toString #:
                JsonNil
              )
              t <- jsonTupleSpace.in(
                complete(
                  string in (Performative.InformDone.name, Performative.Failure.name),
                  "cfp",
                  strCfpId
                )
              )
            } yield t).onComplete {
              case Failure(_) => deleteExecutableAndComplete(executableId, "The executable cannot be allocated.", replyTo)
              case Success(v) =>
                v match {
                  case Performative.InformDone.name #: "cfp" #: strCfpId #: JsonNil =>
                    for {
                      _ <- storage.addExecutableToUser(username, executableId, fileName)
                      _ <- jsonTupleSpace.outAll(
                        proposals
                          .filter(_._1 !== b)
                          .map((u, _) =>
                            Performative.RejectProposal.name #:
                            cfpId.toString #:
                            u.toString #:
                            JsonNil
                          ): _*
                      )
                      _ = Files.delete(Paths.get(executableId.toString))
                      _ = replyTo ! Response.DeployOutput(Success(executableId))
                    } ()
                  case Performative.Failure.name #: "cfp" #: strCfpId #: JsonNil =>
                    ctx.self ! ServiceApiCommand.TryProposals(
                      cfpId,
                      executableId,
                      tpe,
                      proposals.filter(_._1 !== b),
                      username,
                      fileName,
                      replyTo
                    )
                  case _ => deleteExecutableAndComplete(executableId, "The executable cannot be allocated.", replyTo)
                }
            }
          )
          Behaviors.same
        case ServiceApiCommand.Close(id) => main(ctx, storage, jsonTupleSpace, users, sessions - id)
      }
      .receiveSignal {
        case (_, PostStop) =>
          jsonTupleSpace.close()
          Behaviors.same
      }
  }

  /** Factory method for creating a new instance of the [[ServiceApi]] agent actor. It needs the [[ServiceStorage]] to be used as
    * an interface to the effective storage and the [[JsonTupleSpace]] client to communicate to the tuple space.
    *
    * @param storage
    *   the [[ServiceStorage]] instance to be used as an interface to the effective storage
    * @param jsonTupleSpace
    *   the [[JsonTupleSpace]] instance to be used as an interface to the tuple space
    * @return
    *   a new instance of the [[ServiceApi]] agent actor
    */
  def apply(storage: ServiceStorage, jsonTupleSpace: JsonTupleSpace): Behavior[ServiceApiCommand] =
    Behaviors.setup(ctx => {
      println("Master up")
      main(ctx, storage, jsonTupleSpace, Map.empty, Map.empty)
    })
}
