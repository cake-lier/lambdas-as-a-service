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

import scala.util.Try

import laas.worker.model.CallForProposal.CallForProposalId
import laas.worker.model.Executable.{ExecutableId, ExecutableType}
import laas.worker.model.Execution.{ExecutionArguments, ExecutionId, ExecutionOutput}

/** The enum representing all possible messages that can be sent to a [[WorkerAgent]] actor.
  *
  * These messages represents commands that can be sent for notifying the actor that a request for a new operation was received,
  * such as an execution request or a call for proposal request, or the state of a pending request was updated, for example the
  * proposal sent by the actor was accepted or rejected. These messages can also be sent from the [[RunnerAgent]] actor for
  * notifying the [[WorkerAgent]] actor that it started up or it completed its execution job. One last use case is for "message
  * self-sending", where the agent proactively decides to do an action sending a message to itself, for example to keep the
  * startup going after all executable files have been loaded from disk or to terminate a [[RunnerAgent]] when it is no longer
  * needed.
  */
private[agents] enum WorkerAgentCommand {

  /** The message signalling that a [[RunnerAgent]] with assigned an executable file with the given [[ExecutableId]] and
    * [[ExecutableType]] has completed its startup with success.
    *
    * @constructor
    *   creates a new message given the [[ExecutableId]] and the [[ExecutableType]] of the file which was assigned to the
    *   [[RunnerAgent]] which sent this message
    */
  case RunnerUp(id: ExecutableId, tpe: ExecutableType) extends WorkerAgentCommand

    /** The message signalling that the previously requested execution operation, the one with the given [[ExecutionId]], has
      * completed. Its output is the one given, which can be an [[ExecutionOutput]] if the execution completed with success or a
      * [[Throwable]] if it completed with failure.
      *
      * @constructor
      *   creates a new message given the [[ExecutionId]] of the execution operation that was previously requested and the output
      *   of said operation, which is a [[Try]] containing either a [[ExecutionOutput]] or a [[Throwable]]
      */
  case ExecutionComplete(id: ExecutionId, output: Try[ExecutionOutput]) extends WorkerAgentCommand

    /** The message signalling that all executable files where loaded from disk and now the startup of the [[WorkerAgent]] can
      * continue. The message contains the executables, represented as a [[Seq]] of couples of [[ExecutableId]] and
      * [[ExecutableType]].
      *
      * @constructor
      *   creates a new message given the executables that were loaded, represented as a [[Seq]] of couples of [[ExecutableId]]
      *   and [[ExecutableType]] that represents them
      */
  case ExecutablesLoaded(executables: Seq[(ExecutableId, ExecutableType)]) extends WorkerAgentCommand

    /** The message signalling that a new request for execution with the given [[ExecutionId]] was issued. The executable file
      * which was requested to be executed is the one for which the [[ExecutableId]] is given and to the process executing it the
      * given [[ExecutionArguments]] should be passed.
      *
      * @constructor
      *   creates a new message given the [[ExecutionId]] of the requested execution operation, the [[ExecutableId]] of the
      *   executable file to be executed and the [[ExecutionArguments]] to be passed to the process executing it
      */
  case ExecutionRequest(executionId: ExecutionId, executableId: ExecutableId, args: ExecutionArguments) extends WorkerAgentCommand

    /** The message signalling that a new call for proposals was issued with the given id.
      *
      * @constructor
      *   creates a new message with the [[CallForProposalId]] of the new call for proposals that was issued
      */
  case CallForProposal(id: CallForProposalId) extends WorkerAgentCommand

    /** The message signalling that the previously sent proposal for the call for proposals with the given [[CallForProposalId]]
      * was accepted. The executable which is requested to deploy is the one with the given [[ExecutableId]] and
      * [[ExecutableType]].
      *
      * @constructor
      *   creates a new message given the [[CallForProposalId]] of the call for proposals for which the proposal was accepted, the
      *   [[ExecutableId]] and the [[ExecutableType]] of the executable which is now requested to deploy
      */
  case ProposalAccepted(cfpId: CallForProposalId, executableId: ExecutableId, executableType: ExecutableType)
    extends WorkerAgentCommand

    /** The message signalling that the previously sent proposal for the call for proposals with the given [[CallForProposalId]]
      * was rejected.
      *
      * @constructor
      *   creates a new message given the [[CallForProposalId]] of the call for proposals for which the proposal was rejected
      */
  case ProposalRejected(cfpId: CallForProposalId) extends WorkerAgentCommand

    /** The message signalling that the [[RunnerAgent]] actor associated with an executable file with the given [[ExecutableId]]
      * is to be stopped.
      *
      * @constructor
      *   creates a new message given the [[ExecutableId]] of the executable file associated to the [[RunnerAgent]] to be stopped
      */
  case RunnerDown(id: ExecutableId) extends WorkerAgentCommand
}
