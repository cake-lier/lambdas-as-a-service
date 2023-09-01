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
package laas.worker.agents

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import java.util.stream.Collectors

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.Failure
import scala.util.Success

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.DispatcherSelector
import akka.actor.typed.PostStop
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.stream.scaladsl.FileIO

import laas.tuplespace.*
import laas.tuplespace.client.JsonTupleSpace
import laas.worker.agents.WorkerAgentCommand.{RunnerUp, TupleSpaceStarted}
import laas.worker.model.CallForProposal.CallForProposalId
import laas.worker.model.Executable.{ExecutableId, ExecutableType}
import laas.worker.model.Performative
import AnyOps.*

object WorkerAgent {

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  private def launchAwaitExecutionRequest(
    ctx: ActorContext[WorkerAgentCommand],
    tupleSpace: JsonTupleSpace,
    executableId: ExecutableId
  )(
    using
    ExecutionContext
  ): Unit = {
    val strExecutableId = executableId.toString
    tupleSpace
      .in(
        complete(
          Performative.Request.name,
          "execute",
          string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
          strExecutableId,
          string
        )
      )
      .onComplete {
        case Success(
               Performative.Request.name
               #: "execute"
               #: (executionId: String)
               #: strExecutableId
               #: (args: String)
               #: JsonNil
             ) =>
          ctx.self ! WorkerAgentCommand.ExecutionRequest(
            UUID.fromString(executionId),
            executableId,
            args.split(';').toSeq
          )
        case _ => ()
      }
  }

  @SuppressWarnings(Array("org.wartremover.warts.ToString", "org.wartremover.warts.Recursion"))
  private def launchAwaitCallForProposals(
    ctx: ActorContext[WorkerAgentCommand],
    tupleSpace: JsonTupleSpace,
    acceptedExecutableTypes: Seq[ExecutableType],
    callsAlreadyProcessed: Seq[CallForProposalId]
  )(
    using
    ExecutionContext
  ): Unit =
    tupleSpace
      .rd(
        complete(
          Performative.Cfp.name,
          string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
          string.in(acceptedExecutableTypes.map(_.toString): _*)
        )
      )
      .onComplete {
        case Success(Performative.Cfp.name #: (id: String) #: _ #: JsonNil)
             if !callsAlreadyProcessed.contains(UUID.fromString(id)) =>
          ctx.self ! WorkerAgentCommand.CallForProposal(UUID.fromString(id))
        case _ => launchAwaitCallForProposals(ctx, tupleSpace, acceptedExecutableTypes, callsAlreadyProcessed)
      }

  @SuppressWarnings(Array("org.wartremover.warts.ToString", "org.wartremover.warts.Recursion"))
  private def main(
    ctx: ActorContext[WorkerAgentCommand],
    rootActor: ActorRef[RootActorCommand],
    workerId: UUID,
    runnersSpawned: Map[ExecutableId, ActorRef[RunnerAgentCommand]],
    runnersSpawning: Map[ExecutableId, CallForProposalId],
    tupleSpace: JsonTupleSpace,
    downloader: (ExecutableId, ExecutableType) => Future[Unit],
    acceptedExecutableTypes: Seq[ExecutableType],
    slotsAvailable: Int,
    callsAlreadyProcessed: Seq[CallForProposalId]
  )(
    using
    ExecutionContext
  ): Behavior[WorkerAgentCommand] = Behaviors
    .receiveMessage[WorkerAgentCommand] {
      case WorkerAgentCommand.ExecutionComplete(id, output) =>
        output match {
          case Failure(e) => tupleSpace.out(Performative.Failure.name #: "execute" #: id.toString #: e.getMessage #: JsonNil)
          case Success(v) =>
            tupleSpace.out(
              Performative.InformResult.name #:
              "execute" #:
              id.toString #:
              v.exitCode #:
              v.standardOutput #:
              v.standardError #:
              JsonNil
            )
        }
        Behaviors.same
      case WorkerAgentCommand.ExecutionRequest(executionId, executableId, args) =>
        runnersSpawned.get(executableId).foreach(_ ! RunnerAgentCommand.Execute(executionId, args))
        launchAwaitExecutionRequest(ctx, tupleSpace, executableId)
        Behaviors.same
      case WorkerAgentCommand.CallForProposal(id) =>
        if (slotsAvailable > 0) {
          val strId = id.toString
          val strWorkerId = workerId.toString
          tupleSpace
            .out(
              Performative.Propose.name #:
              strId #:
              strWorkerId #:
              slotsAvailable #:
              JsonNil
            )
            .flatMap(_ =>
              tupleSpace.in(
                partial(
                  string in (Performative.RejectProposal.name, Performative.AcceptProposal.name),
                  strId,
                  strWorkerId
                )
              )
            )
            .onComplete {
              case Success(
                     Performative.AcceptProposal.name #:
                     strId #:
                     strWorkerId #:
                     (executableId: String) #:
                     (executableType: String) #:
                     JsonNil
                   ) =>
                ctx.self ! WorkerAgentCommand.ProposalAccepted(
                  id,
                  UUID.fromString(executableId),
                  ExecutableType.valueOf(executableType)
                )
              case Success(
                     Performative.RejectProposal.name #:
                     strId #:
                     strWorkerId #:
                     JsonNil
                   ) =>
                ctx.self ! WorkerAgentCommand.ProposalRejected(id)
              case _ => ()
            }
          launchAwaitCallForProposals(ctx, tupleSpace, acceptedExecutableTypes, callsAlreadyProcessed :+ id)
        } else {
          tupleSpace.out(Performative.Refuse.name #: id.toString #: JsonNil)
        }
        Behaviors.same
      case WorkerAgentCommand.ProposalRejected(cfpId) =>
        main(
          ctx,
          rootActor,
          workerId,
          runnersSpawned,
          runnersSpawning,
          tupleSpace,
          downloader,
          acceptedExecutableTypes,
          slotsAvailable,
          callsAlreadyProcessed.filter(_ !== cfpId)
        )
      case WorkerAgentCommand.ProposalAccepted(cfpId, executableId, executableType) =>
        if (slotsAvailable > 0) {
          main(
            ctx,
            rootActor,
            workerId,
            runnersSpawned +
            (executableId -> ctx.spawn(RunnerAgent(ctx.self, executableId, executableType), executableId.toString)),
            runnersSpawning + (executableId -> cfpId),
            tupleSpace,
            downloader,
            acceptedExecutableTypes,
            slotsAvailable - 1,
            callsAlreadyProcessed.filter(_ !== cfpId)
          )
        } else {
          tupleSpace.out(Performative.Failure.name #: "cfp" #: cfpId.toString #: JsonNil)
          main(
            ctx,
            rootActor,
            workerId,
            runnersSpawned,
            runnersSpawning,
            tupleSpace,
            downloader,
            acceptedExecutableTypes,
            slotsAvailable,
            callsAlreadyProcessed.filter(_ !== cfpId)
          )
        }
      case WorkerAgentCommand.RunnerUp(id, tpe) =>
        runnersSpawning
          .get(id)
          .foreach(i =>
            downloader(id, tpe).onComplete {
              case Failure(_) =>
                ctx.self ! WorkerAgentCommand.RunnerDown(id)
                tupleSpace.out(Performative.Failure.name #: "cfp" #: i.toString #: JsonNil)
              case Success(_) =>
                launchAwaitExecutionRequest(ctx, tupleSpace, id)
                tupleSpace.out(Performative.InformDone.name #: "cfp" #: i.toString #: JsonNil)
            }
          )
        main(
          ctx,
          rootActor,
          workerId,
          runnersSpawned,
          runnersSpawning - id,
          tupleSpace,
          downloader,
          acceptedExecutableTypes,
          slotsAvailable,
          callsAlreadyProcessed
        )
      case WorkerAgentCommand.RunnerDown(id) =>
        runnersSpawned.get(id).foreach(ctx.stop)
        main(
          ctx,
          rootActor,
          workerId,
          runnersSpawned,
          runnersSpawning,
          tupleSpace,
          downloader,
          acceptedExecutableTypes,
          slotsAvailable + 1,
          callsAlreadyProcessed
        )
      case _ => Behaviors.ignore
    }
    .receiveSignal {
      case (_, PostStop) =>
        tupleSpace.close()
        Behaviors.same
    }

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  private def setup(
    ctx: ActorContext[WorkerAgentCommand],
    rootActor: ActorRef[RootActorCommand],
    workerId: UUID,
    runnersSpawned: Map[ExecutableId, ActorRef[RunnerAgentCommand]],
    tupleSpace: JsonTupleSpace,
    downloader: (ExecutableId, ExecutableType) => Future[Unit],
    acceptedExecutableTypes: Seq[ExecutableType],
    slotsAvailable: Int
  )(
    using
    ExecutionContext
  ): Behavior[WorkerAgentCommand] = {
    rootActor ! RootActorCommand.WorkerUp(success = true)
    runnersSpawned.keys.foreach(launchAwaitExecutionRequest(ctx, tupleSpace, _))
    launchAwaitCallForProposals(ctx, tupleSpace, acceptedExecutableTypes, Seq.empty)
    println(s"Worker ${workerId.toString} up")
    main(
      ctx,
      rootActor,
      workerId,
      runnersSpawned,
      Map.empty,
      tupleSpace,
      downloader,
      acceptedExecutableTypes,
      slotsAvailable,
      Seq.empty
    )
  }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def awaitRunnerStart(
    remainingRunnersSpawned: Int,
    runnersSpawned: Map[UUID, ActorRef[RunnerAgentCommand]],
    ctx: ActorContext[WorkerAgentCommand],
    rootActor: ActorRef[RootActorCommand],
    workerId: UUID,
    tupleSpace: JsonTupleSpace,
    downloader: (ExecutableId, ExecutableType) => Future[Unit],
    acceptedExecutableTypes: Seq[ExecutableType],
    slotsAvailable: Int
  )(
    using
    ExecutionContext
  ): Behavior[WorkerAgentCommand] = Behaviors.receiveMessage {
    case WorkerAgentCommand.RunnerUp(_, _) if remainingRunnersSpawned > 1 =>
      awaitRunnerStart(
        remainingRunnersSpawned - 1,
        runnersSpawned,
        ctx,
        rootActor,
        workerId,
        tupleSpace,
        downloader,
        acceptedExecutableTypes,
        slotsAvailable
      )
    case WorkerAgentCommand.RunnerUp(_, _) =>
      setup(ctx, rootActor, workerId, runnersSpawned, tupleSpace, downloader, acceptedExecutableTypes, slotsAvailable)
    case _ => Behaviors.ignore
  }

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  def apply(
    rootActor: ActorRef[RootActorCommand],
    workerId: UUID,
    tupleSpace: JsonTupleSpace,
    downloader: (ExecutableId, ExecutableType) => Future[Unit],
    acceptedExecutableTypes: Seq[ExecutableType],
    slotsAvailable: Int
  ): Behavior[WorkerAgentCommand] = Behaviors.setup { ctx =>
    given ExecutionContext = ctx.system.dispatchers.lookup(DispatcherSelector.blocking())
    Future {
      Files
        .list(Paths.get("executables"))
        .collect(Collectors.toList)
        .asScala
        .toSeq
        .flatMap(p => {
          val nameParts = p.getFileName.toString.split('.')
          if (
            nameParts.length === 2 &&
            nameParts(0).matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$") &&
            ExecutableType.values.map(_.extension).toSeq.contains(nameParts(1))
          )
            Some((UUID.fromString(nameParts(0)), ExecutableType.getByExtension(nameParts(1))))
          else
            None
        })
    }.onComplete {
      case Failure(_) => rootActor ! RootActorCommand.WorkerUp(success = false)
      case Success(v) => ctx.self ! WorkerAgentCommand.ExecutablesLoaded(v)
    }
    Behaviors.receiveMessage {
      case WorkerAgentCommand.ExecutablesLoaded(v) =>
        val runnersSpawned = v.map((i, t) => (i, ctx.spawn(RunnerAgent(ctx.self, i, t), i.toString))).toMap
        if (runnersSpawned.nonEmpty)
          awaitRunnerStart(
            runnersSpawned.size,
            runnersSpawned,
            ctx,
            rootActor,
            workerId,
            tupleSpace,
            downloader,
            acceptedExecutableTypes,
            slotsAvailable
          )
        else
          setup(
            ctx,
            rootActor,
            workerId,
            runnersSpawned,
            tupleSpace,
            downloader,
            acceptedExecutableTypes,
            slotsAvailable
          )
      case _ => Behaviors.ignore
    }
  }
}
