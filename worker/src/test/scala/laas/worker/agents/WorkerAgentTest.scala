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

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Failure

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.testcontainers.containers.wait.strategy.Wait

import laas.tuplespace.*
import laas.tuplespace.client.JsonTupleSpace
import laas.worker.model.Executable.ExecutableType
import laas.worker.model.Execution.ExecutionOutput
import laas.worker.model.Performative

@SuppressWarnings(
  Array(
    "org.wartremover.warts.ToString",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.GlobalExecutionContext",
    "scalafix:DisableSyntax.var"
  )
)
class WorkerAgentTest extends AnyFunSpec with BeforeAndAfterAll with TestContainerForAll {

  override val containerDef: GenericContainer.Def[GenericContainer] = GenericContainer.Def(
    "matteocastellucci3/laas-ts-server:latest",
    exposedPorts = Seq(80),
    waitStrategy = Wait.forListeningPort()
  )

  private given ExecutionContext = scala.concurrent.ExecutionContext.global

  private val testKit = ActorTestKit()
  private val rootActorProbe = testKit.createTestProbe[RootActorCommand]()
  private val workerId = UUID.randomUUID()
  private var tupleSpace: Option[JsonTupleSpace] = None

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    Files.createDirectory(Paths.get("executables"))
  }

  override def afterContainersStart(containers: containerDef.Container): Unit =
    tupleSpace = Some(
      Await.result(
        JsonTupleSpace("ws://localhost:" + containers.container.getFirstMappedPort.toString + "/tuplespace"),
        30.seconds
      )
    )

  override protected def afterAll(): Unit = {
    super.afterAll()
    testKit.shutdownTestKit()
    Files.delete(Paths.get("executables"))
  }

  describe("A worker agent") {
    describe("when first booted up") {
      it("should notify its root actor") {
        val workerAgent = testKit.spawn(
          WorkerAgent(
            rootActorProbe.ref,
            workerId,
            tupleSpace.getOrElse(fail()),
            _ => Future.successful(()),
            Seq(ExecutableType.Java),
            1
          )
        )
        rootActorProbe.expectMessage(RootActorCommand.WorkerUp(success = true))
        testKit.stop(workerAgent)
      }
    }

    describe("when is notified that an execution is requested") {
      it("should notify its success on the tuples space") {
        val executableId = UUID.randomUUID()
        Files.copy(
          Paths.get("src", "test", "resources", "exec.jar"),
          Paths.get("executables", executableId.toString + ".jar")
        )
        val workerAgent = testKit.spawn(
          WorkerAgent(
            rootActorProbe.ref,
            workerId,
            tupleSpace.getOrElse(fail()),
            _ => Future.successful(()),
            Seq(ExecutableType.Java),
            1
          )
        )
        val executionId = UUID.randomUUID()
        Await.ready(
          for {
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.Request.name #:
                "execute" #:
                executionId.toString #:
                executableId.toString #:
                "out;err" #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.InformResult.name,
                  "execute",
                  executionId.toString,
                  0,
                  "out\n",
                  "err\n"
                )
              )
          } yield (),
          30.seconds
        )
        testKit.stop(workerAgent)
        Files.delete(Paths.get("executables", executableId.toString + ".jar"))
      }
    }

    describe("when is notified that an execution has completed with failure") {
      it("should notify it to the tuples space") {
        val workerAgent = testKit.spawn(
          WorkerAgent(
            rootActorProbe.ref,
            workerId,
            tupleSpace.getOrElse(fail()),
            _ => Future.successful(()),
            Seq(ExecutableType.Java),
            1
          )
        )
        val executionId = UUID.randomUUID()
        val output = ExecutionOutput(0, "out\n", "err\n")
        workerAgent ! WorkerAgentCommand.ExecutionComplete(executionId, Failure(RuntimeException("Generic error")))
        Await.ready(
          tupleSpace
            .getOrElse(fail())
            .in(
              complete(
                Performative.Failure.name,
                "execute",
                executionId.toString,
                "Generic error"
              )
            ),
          30.seconds
        )
        testKit.stop(workerAgent)
      }
    }

    describe("when a call for proposal is issued but no more slots are available") {
      it("should refuse the request") {
        val workerAgent = testKit.spawn(
          WorkerAgent(
            rootActorProbe.ref,
            workerId,
            tupleSpace.getOrElse(fail()),
            _ => Future.successful(()),
            Seq(ExecutableType.Java),
            0
          )
        )
        val cfpId = UUID.randomUUID()
        Await.ready(
          for {
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.Cfp.name #:
                cfpId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Refuse.name,
                  cfpId.toString
                )
              )
          } yield (),
          30.seconds
        )
        testKit.stop(workerAgent)
      }
    }

    describe("when a call for proposal is issued but the proposal request is rejected") {
      it("should do nothing") {
        val workerAgent = testKit.spawn(
          WorkerAgent(
            rootActorProbe.ref,
            workerId,
            tupleSpace.getOrElse(fail()),
            _ => Future.successful(()),
            Seq(ExecutableType.Java),
            1
          )
        )
        val cfpId = UUID.randomUUID()
        Await.ready(
          for {
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.Cfp.name #:
                cfpId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Propose.name,
                  cfpId.toString,
                  workerId.toString,
                  1
                )
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.RejectProposal.name #:
                cfpId.toString #:
                workerId.toString #:
                JsonNil
              )
          } yield (),
          30.seconds
        )
        testKit.stop(workerAgent)
      }
    }

    describe("when a call for proposal is issued, the request accepted and the executable transferred") {
      it("should notify it to the tuples space") {
        val workerAgent = testKit.spawn(
          WorkerAgent(
            rootActorProbe.ref,
            workerId,
            tupleSpace.getOrElse(fail()),
            _ => Future.successful(()),
            Seq(ExecutableType.Java),
            1
          )
        )
        val cfpId = UUID.randomUUID()
        val executableId = UUID.randomUUID()
        Await.ready(
          for {
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.Cfp.name #:
                cfpId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Propose.name,
                  cfpId.toString,
                  workerId.toString,
                  1
                )
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.AcceptProposal.name #:
                cfpId.toString #:
                workerId.toString #:
                executableId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.InformDone.name,
                  "cfp",
                  cfpId.toString
                )
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.Cfp.name #:
                cfpId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Refuse.name,
                  cfpId.toString
                )
              )
          } yield (),
          30.seconds
        )
        testKit.stop(workerAgent)
      }
    }

    describe("when a call for proposal is issued, the request accepted but the executable not transferred") {
      it("should notify it to the tuples space") {
        val workerAgent = testKit.spawn(
          WorkerAgent(
            rootActorProbe.ref,
            workerId,
            tupleSpace.getOrElse(fail()),
            _ => Future.failed[Unit](RuntimeException()),
            Seq(ExecutableType.Java),
            1
          )
        )
        val cfpId = UUID.randomUUID()
        val executableId = UUID.randomUUID()
        Await.ready(
          for {
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.Cfp.name #:
                cfpId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Propose.name,
                  cfpId.toString,
                  workerId.toString,
                  1
                )
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.AcceptProposal.name #:
                cfpId.toString #:
                workerId.toString #:
                executableId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Failure.name,
                  "cfp",
                  cfpId.toString
                )
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.Cfp.name #:
                cfpId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Propose.name,
                  cfpId.toString,
                  workerId.toString,
                  1
                )
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.AcceptProposal.name #:
                cfpId.toString #:
                workerId.toString #:
                executableId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.InformDone.name,
                  "cfp",
                  cfpId.toString
                )
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.Cfp.name #:
                cfpId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Refuse.name,
                  cfpId.toString
                )
              )
          } yield (),
          30.seconds
        )
        testKit.stop(workerAgent)
      }
    }

    describe("when two calls for proposal are issued, but only one slot is available") {
      it("should accept only one request and refuse the other") {
        val workerAgent = testKit.spawn(
          WorkerAgent(
            rootActorProbe.ref,
            workerId,
            tupleSpace.getOrElse(fail()),
            _ => Future.successful(()),
            Seq(ExecutableType.Java),
            1
          )
        )
        val cfpId1 = UUID.randomUUID()
        val cfpId2 = UUID.randomUUID()
        val executableId = UUID.randomUUID()
        Await.ready(
          for {
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.Cfp.name #:
                cfpId1.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.Cfp.name #:
                cfpId2.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Propose.name,
                  cfpId1.toString,
                  workerId.toString,
                  1
                )
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Propose.name,
                  cfpId2.toString,
                  workerId.toString,
                  1
                )
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.AcceptProposal.name #:
                cfpId1.toString #:
                workerId.toString #:
                executableId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.InformDone.name,
                  "cfp",
                  cfpId1.toString
                )
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .out(
                Performative.AcceptProposal.name #:
                cfpId2.toString #:
                workerId.toString #:
                executableId.toString #:
                ExecutableType.Java.toString #:
                JsonNil
              )
            _ <- tupleSpace
              .getOrElse(fail())
              .in(
                complete(
                  Performative.Failure.name,
                  "cfp",
                  cfpId2.toString
                )
              )
          } yield (),
          30.seconds
        )
        testKit.stop(workerAgent)
      }
    }
  }
}
