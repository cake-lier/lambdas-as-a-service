package io.github.cakelier
package laas.worker.agents

import laas.worker.model.Executable.{ExecutableId, ExecutableType}
import laas.worker.model.Execution.{ExecutionArguments, ExecutionOutput}

import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors

import scala.sys.process.*
import scala.concurrent.{ExecutionContext, Future}
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

object RunnerAgent {

  @SuppressWarnings(Array("org.wartremover.warts.ToString", "org.wartremover.warts.Null"))
  def apply(worker: ActorRef[WorkerAgentCommand], executableId: ExecutableId, tpe: ExecutableType): Behavior[RunnerAgentCommand] =
    Behaviors.setup { ctx =>
      worker ! WorkerAgentCommand.RunnerUp
      given ExecutionContext = ctx.system.dispatchers.lookup(DispatcherSelector.blocking())
      Behaviors.receiveMessage {
        case RunnerAgentCommand.Execute(id, args) => Future {
          val stdout = Files.createTempFile(executableId.toString, null)
          val stderr = Files.createTempFile(executableId.toString, null)
          val executionOutput = tpe match {
            case ExecutableType.Java => exec(Seq("java", "-jar", executableId.toString + ".jar"), args, stdout, stderr)
            case ExecutableType.Python => exec(Seq("python", executableId.toString + ".py"), args, stdout, stderr)
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
        .run(ProcessLogger(
          s => Files.writeString(stdout, s + "\n", StandardOpenOption.APPEND),
          s => Files.writeString(stderr, s + "\n", StandardOpenOption.APPEND)
        ))
        .exitValue()
    ExecutionOutput(exitCode, Files.readString(stdout), Files.readString(stderr))
  }
}
