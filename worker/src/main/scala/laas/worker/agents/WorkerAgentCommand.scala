package io.github.cakelier
package laas.worker.agents

import laas.worker.model.Execution.{ExecutionId, ExecutionOutput}

import scala.util.Try

enum WorkerAgentCommand {

  case RunnerUp extends WorkerAgentCommand

  case ExecutionComplete(id: ExecutionId, output: Try[ExecutionOutput]) extends WorkerAgentCommand
}