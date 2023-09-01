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
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.sys.process.*

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.DispatcherSelector
import akka.actor.typed.scaladsl.Behaviors

import laas.worker.model.Executable.{ExecutableId, ExecutableType}
import laas.worker.model.Execution.{ExecutionArguments, ExecutionOutput}

object RunnerAgent {

  @SuppressWarnings(Array("org.wartremover.warts.ToString", "org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
  def apply(worker: ActorRef[WorkerAgentCommand], executableId: ExecutableId, tpe: ExecutableType): Behavior[RunnerAgentCommand] =
    Behaviors.setup { ctx =>
      worker ! WorkerAgentCommand.RunnerUp(executableId, tpe)
      given ExecutionContext = ctx.system.dispatchers.lookup(DispatcherSelector.blocking())
      println(s"Runner ${executableId.toString} up")
      Behaviors.receiveMessage {
        case RunnerAgentCommand.Execute(id, args) =>
          Future {
            val stdout = Files.createTempFile(executableId.toString, null)
            val stderr = Files.createTempFile(executableId.toString, null)
            val executionOutput = tpe match {
              case ExecutableType.Java =>
                exec(
                  Seq("java", "-jar", Paths.get("executables", executableId.toString + ".jar").toString),
                  args,
                  stdout,
                  stderr
                )
              case ExecutableType.Python =>
                exec(
                  Seq("python", Paths.get("executables", executableId.toString + ".py").toString),
                  args,
                  stdout,
                  stderr
                )
            }
            Files.delete(stdout)
            Files.delete(stderr)
            executionOutput
          }.onComplete(worker ! WorkerAgentCommand.ExecutionComplete(id, _))(ctx.executionContext)
          Behaviors.same
      }
    }

  private def exec(launchCommand: Seq[String], args: ExecutionArguments, stdout: Path, stderr: Path): ExecutionOutput = {
    val exitCode =
      (launchCommand ++ args)
        .run(
          ProcessLogger(
            s => Files.writeString(stdout, s + "\n", StandardOpenOption.APPEND),
            s => Files.writeString(stderr, s + "\n", StandardOpenOption.APPEND)
          )
        )
        .exitValue()
    ExecutionOutput(exitCode, Files.readString(stdout), Files.readString(stderr))
  }
}
