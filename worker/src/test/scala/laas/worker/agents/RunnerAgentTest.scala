package io.github.cakelier
package laas.worker.agents

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.TryValues.*
import laas.worker.model.Executable.ExecutableType
import laas.worker.model.Execution.ExecutionOutput
import laas.worker.agents.RunnerAgentCommand

import java.util.UUID
import java.nio.file.{Files, Paths}
import scala.util.{Failure, Success}
import scala.reflect.ClassTag

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
class RunnerAgentTest extends AnyFunSpec with BeforeAndAfterAll {

  private val testKit = ActorTestKit()
  private val workerActorProbe = testKit.createTestProbe[WorkerAgentCommand]()
  private val executableId = UUID.randomUUID()

  override protected def beforeAll(): Unit = {
    Files.copy(Paths.get("worker", "src", "test", "resources", "exec.jar"), Paths.get(executableId.toString + ".jar"))
  }

  override protected def afterAll(): Unit = {
    testKit.shutdownTestKit()
    Files.delete(Paths.get(executableId.toString + ".jar"))
  }

  describe("A runner agent") {
    describe("when first booted up") {
      it("should inform its worker agent") {
        testKit.spawn(RunnerAgent(workerActorProbe.ref, executableId, ExecutableType.Java))
        workerActorProbe.expectMessage(WorkerAgentCommand.RunnerUp)
      }
    }

    describe("after receiving the correct arguments") {
      it("should return the execution results") {
        val runnerAgent = testKit.spawn(RunnerAgent(workerActorProbe.ref, executableId, ExecutableType.Java))
        workerActorProbe.expectMessage(WorkerAgentCommand.RunnerUp)
        val executionId = UUID.randomUUID()
        runnerAgent ! RunnerAgentCommand.Execute(executionId, Seq("out", "err"))
        val executionComplete: WorkerAgentCommand.ExecutionComplete =
          workerActorProbe.expectMessageType(
            ClassTag.apply[WorkerAgentCommand.ExecutionComplete](classOf[WorkerAgentCommand.ExecutionComplete])
          )
        executionComplete.id shouldBe executionId
        val output = executionComplete.output.success.value
        output.exitCode shouldBe 0
        output.standardOutput shouldBe "out\n"
        output.standardError should endWith ("err\n")
      }
    }

    describe("after receiving the wrong arguments") {
      it("should return an exception") {
        val runnerAgent = testKit.spawn(RunnerAgent(workerActorProbe.ref, executableId, ExecutableType.Java))
        workerActorProbe.expectMessage(WorkerAgentCommand.RunnerUp)
        val executionId = UUID.randomUUID()
        runnerAgent ! RunnerAgentCommand.Execute(executionId, Seq("out\n"))
        val executionComplete: WorkerAgentCommand.ExecutionComplete =
          workerActorProbe.expectMessageType(
            ClassTag.apply[WorkerAgentCommand.ExecutionComplete](classOf[WorkerAgentCommand.ExecutionComplete])
          )
        executionComplete.id shouldBe executionId
        val output = executionComplete.output.success.value
        output.exitCode shouldBe 1
        output.standardOutput shouldBe "out\n"
        output.standardError should endWith(
          s"""Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
              |\tat it.unibo.ds.laas.script.Main.main(Main.java:6)
              |""".stripMargin
        )
      }
    }

    describe("if its executable is missing") {
      it("should fail while trying to execute") {
        val wrongExecutionId = UUID.randomUUID()
        val runnerAgent = testKit.spawn(RunnerAgent(workerActorProbe.ref, wrongExecutionId, ExecutableType.Java))
        workerActorProbe.expectMessage(WorkerAgentCommand.RunnerUp)
        val executionId = UUID.randomUUID()
        runnerAgent ! RunnerAgentCommand.Execute(executionId, Seq("out", "err"))
        val executionComplete: WorkerAgentCommand.ExecutionComplete =
          workerActorProbe.expectMessageType(
            ClassTag.apply[WorkerAgentCommand.ExecutionComplete](classOf[WorkerAgentCommand.ExecutionComplete])
          )
        executionComplete.id shouldBe executionId
        val output = executionComplete.output.success.value
        output.exitCode shouldBe 1
        output.standardOutput shouldBe ""
        output.standardError should endWith(s"Error: Unable to access jarfile $wrongExecutionId.jar\n")
      }
    }
  }
}
