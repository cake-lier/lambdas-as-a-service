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

import scala.reflect.ClassTag
import scala.util.Failure
import scala.util.Success

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.TryValues.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import laas.worker.model.Executable.ExecutableType
import laas.worker.model.Execution.ExecutionOutput
import laas.worker.agents.RunnerAgentCommand

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
class RunnerAgentTest extends AnyFunSpec with BeforeAndAfterAll {

  private val testKit = ActorTestKit()
  private val workerActorProbe = testKit.createTestProbe[WorkerAgentCommand]()
  private val executableId = UUID.randomUUID()

  override protected def beforeAll(): Unit = {
    Files.createDirectory(Paths.get("executables"))
    Files.copy(
      Paths.get("src", "test", "resources", "exec.jar"),
      Paths.get("executables", executableId.toString + ".jar")
    )
  }

  override protected def afterAll(): Unit = {
    testKit.shutdownTestKit()
    Files.delete(Paths.get("executables", executableId.toString + ".jar"))
    Files.delete(Paths.get("executables"))
  }

  describe("A runner agent") {
    describe("when first booted up") {
      it("should inform its worker agent") {
        val runnerAgent = testKit.spawn(RunnerAgent(workerActorProbe.ref, executableId, ExecutableType.Java))
        workerActorProbe.expectMessage(WorkerAgentCommand.RunnerUp(executableId, ExecutableType.Java))
        testKit.stop(runnerAgent)
      }
    }

    describe("after receiving the correct arguments") {
      it("should return the execution results") {
        val runnerAgent = testKit.spawn(RunnerAgent(workerActorProbe.ref, executableId, ExecutableType.Java))
        workerActorProbe.expectMessage(WorkerAgentCommand.RunnerUp(executableId, ExecutableType.Java))
        val executionId = UUID.randomUUID()
        runnerAgent ! RunnerAgentCommand.Execute(executionId, Seq("out", "err"))
        val executionComplete: WorkerAgentCommand.ExecutionComplete =
          workerActorProbe.expectMessageType[WorkerAgentCommand.ExecutionComplete]
        executionComplete.id shouldBe executionId
        val output = executionComplete.output.success.value
        output.exitCode shouldBe 0
        output.standardOutput shouldBe "out\n"
        output.standardError should endWith("err\n")
        testKit.stop(runnerAgent)
      }
    }

    describe("after receiving the wrong arguments") {
      it("should return an exception") {
        val runnerAgent = testKit.spawn(RunnerAgent(workerActorProbe.ref, executableId, ExecutableType.Java))
        workerActorProbe.expectMessage(WorkerAgentCommand.RunnerUp(executableId, ExecutableType.Java))
        val executionId = UUID.randomUUID()
        runnerAgent ! RunnerAgentCommand.Execute(executionId, Seq("out\n"))
        val executionComplete: WorkerAgentCommand.ExecutionComplete =
          workerActorProbe.expectMessageType[WorkerAgentCommand.ExecutionComplete]
        executionComplete.id shouldBe executionId
        val output = executionComplete.output.success.value
        output.exitCode shouldBe 1
        output.standardOutput shouldBe "out\n"
        output.standardError should endWith(
          "Exception in thread \"main\" java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1\n" +
          "\tat it.unibo.ds.laas.script.Main.main(Main.java:6)\n"
        )
        testKit.stop(runnerAgent)
      }
    }

    describe("if its executable is missing") {
      it("should fail while trying to execute") {
        val wrongExecutionId = UUID.randomUUID()
        val runnerAgent = testKit.spawn(RunnerAgent(workerActorProbe.ref, wrongExecutionId, ExecutableType.Java))
        workerActorProbe.expectMessage(WorkerAgentCommand.RunnerUp(wrongExecutionId, ExecutableType.Java))
        val executionId = UUID.randomUUID()
        runnerAgent ! RunnerAgentCommand.Execute(executionId, Seq("out", "err"))
        val executionComplete: WorkerAgentCommand.ExecutionComplete =
          workerActorProbe.expectMessageType[WorkerAgentCommand.ExecutionComplete]
        executionComplete.id shouldBe executionId
        val output = executionComplete.output.success.value
        output.exitCode shouldBe 1
        output.standardOutput shouldBe ""
        output.standardError should endWith(s"Error: Unable to access jarfile executables/$wrongExecutionId.jar\n")
        testKit.stop(runnerAgent)
      }
    }
  }
}
