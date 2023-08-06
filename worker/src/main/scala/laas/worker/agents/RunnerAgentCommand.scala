package io.github.cakelier
package laas.worker.agents

import java.util.UUID

import laas.worker.model.Execution.{ExecutionArguments, ExecutionId}

enum RunnerAgentCommand {

  case Execute(id: ExecutionId, args: ExecutionArguments) extends RunnerAgentCommand
}
