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

import java.util.UUID

import laas.worker.model.Execution.{ExecutionArguments, ExecutionId}

/** The enum representing all possible messages that can be sent to a [[RunnerAgent]].
  *
  * The only message that can be sent is to execute a new executable.
  */
private[agents] enum RunnerAgentCommand {

  /** The message signalling that a new execution operation with the given id was requested and the arguments to be passed to the
    * process launching it are the ones given.
    *
    * @constructor
    *   creates a new message given the id of the new execution operation to perform and the arguments to the process that should
    *   launch it
    */
  case Execute(id: ExecutionId, args: ExecutionArguments) extends RunnerAgentCommand
}
