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

  private def launchAwaitExecutionRequest(
    ctx: ActorContext[WorkerAgentCommand],
    tupleSpace: JsonTupleSpace,
    runnersSpawnedNames: Seq[UUID]
  ): Unit = {
    given ExecutionContext = ctx.system.dispatchers.lookup(DispatcherSelector.blocking())
    tupleSpace
      .in(
        complete(
          Performative.Request.name,
          "execute",
          string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
          string.in(runnersSpawnedNames.map(_.toString): _*),
          string
        )
      )
      .onComplete {
        case Success(
               Performative.Request.name
               #: "execute"
               #: (executionId: String)
               #: (executableId: String)
               #: (args: String)
               #: JsonNil
             ) =>
          ctx.self ! WorkerAgentCommand.ExecutionRequest(
            UUID.fromString(executionId),
            UUID.fromString(executableId),
            args.split(';').toSeq
          )
        case _ => ()
      }
  }

  private def launchAwaitCallForProposals(
    ctx: ActorContext[WorkerAgentCommand],
    tupleSpace: JsonTupleSpace,
    acceptedExecutableTypes: Seq[ExecutableType]
  ): Unit = {
    given ExecutionContext = ctx.system.dispatchers.lookup(DispatcherSelector.blocking())
    tupleSpace
      .rd(
        complete(
          Performative.Cfp.name,
          string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
          string.in(acceptedExecutableTypes.map(_.toString): _*)
        )
      )
      .onComplete {
        case Success(Performative.Cfp.name #: (id: String) #: JsonNil) =>
          ctx.self ! WorkerAgentCommand.CallForProposal(UUID.fromString(id))
        case _ => ()
      }
  }

  @SuppressWarnings(Array("org.wartremover.warts.ToString", "org.wartremover.warts.Recursion"))
  private def main(
    ctx: ActorContext[WorkerAgentCommand],
    rootActor: ActorRef[RootActorCommand],
    workerId: UUID,
    runnersSpawned: Map[ExecutableId, ActorRef[RunnerAgentCommand]],
    runnersSpawning: Map[ExecutableId, CallForProposalId],
    tupleSpace: JsonTupleSpace,
    downloader: ExecutableId => Future[Unit],
    acceptedExecutableTypes: Seq[ExecutableType],
    slotsAvailable: Int
  ): Behavior[WorkerAgentCommand] = {
    given ExecutionContext = ctx.system.dispatchers.lookup(DispatcherSelector.blocking())
    rootActor ! RootActorCommand.WorkerUp(success = true)
    launchAwaitExecutionRequest(ctx, tupleSpace, runnersSpawned.keys.toSeq)
    launchAwaitCallForProposals(ctx, tupleSpace, acceptedExecutableTypes)
    Behaviors.receiveMessage[WorkerAgentCommand] {
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
        launchAwaitExecutionRequest(ctx, tupleSpace, runnersSpawned.keys.toSeq)
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
              case _ => ()
            }
          launchAwaitCallForProposals(ctx, tupleSpace, acceptedExecutableTypes)
        } else {
          tupleSpace.out(Performative.Refuse.name #: id.toString #: JsonNil)
        }
        Behaviors.same
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
            slotsAvailable - 1
          )
        } else {
          tupleSpace.out(Performative.Failure.name #: "cfp" #: cfpId.toString #: JsonNil)
          Behaviors.same
        }
      case WorkerAgentCommand.RunnerUp(id) =>
        runnersSpawning
          .get(id)
          .foreach(i =>
            downloader(id).onComplete {
              case Failure(_) =>
                ctx.self ! WorkerAgentCommand.RunnerDown(id)
                tupleSpace.out(Performative.Failure.name #: "cfp" #: i.toString #: JsonNil)
              case Success(_) => tupleSpace.out(Performative.InformDone.name #: "cfp" #: i.toString #: JsonNil)
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
          slotsAvailable
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
          slotsAvailable + 1
        )
      case _ => Behaviors.ignore
    }
  }.receiveSignal {
    case (_, PostStop) =>
      tupleSpace.close()
      Behaviors.same
  }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def awaitRunnerStart(
    remainingRunnersSpawned: Int,
    runnersSpawned: Map[UUID, ActorRef[RunnerAgentCommand]],
    ctx: ActorContext[WorkerAgentCommand],
    rootActor: ActorRef[RootActorCommand],
    workerId: UUID,
    tupleSpace: JsonTupleSpace,
    downloader: ExecutableId => Future[Unit],
    acceptedExecutableTypes: Seq[ExecutableType],
    slotsAvailable: Int
  ): Behavior[WorkerAgentCommand] = Behaviors.receiveMessage {
    case WorkerAgentCommand.RunnerUp(_) if remainingRunnersSpawned > 1 =>
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
    case WorkerAgentCommand.RunnerUp(_) =>
      main(ctx, rootActor, workerId, runnersSpawned, Map.empty, tupleSpace, downloader, acceptedExecutableTypes, slotsAvailable)
    case _ => Behaviors.ignore
  }

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  def apply(
    rootActor: ActorRef[RootActorCommand],
    workerId: UUID,
    tupleSpace: JsonTupleSpace,
    downloader: ExecutableId => Future[Unit],
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
            Some((UUID.fromString(nameParts(0)), ExecutableType.valueOf(nameParts(1))))
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
          main(
            ctx,
            rootActor,
            workerId,
            runnersSpawned,
            Map.empty,
            tupleSpace,
            downloader,
            acceptedExecutableTypes,
            slotsAvailable
          )
      case _ => Behaviors.ignore
    }
  }
}
