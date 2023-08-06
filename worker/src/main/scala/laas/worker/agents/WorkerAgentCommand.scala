package io.github.cakelier
package laas.worker.agents

import scala.util.Try

import laas.tuplespace.client.JsonTupleSpace
import laas.worker.model.CallForProposal.CallForProposalId
import laas.worker.model.Executable.{ExecutableId, ExecutableType}
import laas.worker.model.Execution.{ExecutionArguments, ExecutionId, ExecutionOutput}

enum WorkerAgentCommand {

  case RunnerUp(id: ExecutableId) extends WorkerAgentCommand

  case ExecutionComplete(id: ExecutionId, output: Try[ExecutionOutput]) extends WorkerAgentCommand

  case TupleSpaceStarted(tupleSpace: JsonTupleSpace) extends WorkerAgentCommand

  case ExecutablesLoaded(executables: Seq[(ExecutableId, ExecutableType)]) extends WorkerAgentCommand

  case ExecutionRequest(executionId: ExecutionId, executableId: ExecutableId, args: ExecutionArguments) extends WorkerAgentCommand

  case CallForProposal(id: CallForProposalId) extends WorkerAgentCommand

  case ProposalAccepted(cfpId: CallForProposalId, executableId: ExecutableId, executableType: ExecutableType)
    extends WorkerAgentCommand

  case RunnerDown(id: ExecutableId) extends WorkerAgentCommand
}
