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
package laas.worker.model

import java.util.UUID

/** The object containing all domain entities regarding the operation which executes an executable file, an "execution". */
private[worker] object Execution {

  /** The identifier which represents uniquely an execution operation in the system. */
  type ExecutionId = UUID

  /** The type of the arguments passed as input to the execution process to be executed. */
  type ExecutionArguments = Seq[String]

  /** The output of an execution, which consists of the exit code of the process which executed the executable file, what was
    * printed on standard output and what was printed on standard error.
    */
  trait ExecutionOutput {

    /** Returns the exit code of the process which executed a given executable file. */
    val exitCode: Int

    /** Returns what was written on standard output during the process execution. */
    val standardOutput: String

    /** Returns what was written on standard error during the process execution. */
    val standardError: String
  }

  /** Companion object to the [[ExecutionOutput]] trait, containing its factory method. */
  object ExecutionOutput {

    /* Implementation of the ExecutionOutput trait. */
    private case class ExecutionOutputImpl(exitCode: Int, standardOutput: String, standardError: String) extends ExecutionOutput

      /** Factory method able to create a new instance of the [[ExecutionOutput]] trait given the exit code, the standard output
        * and the standard error values which will be contained in the new instance.
        *
        * @param exitCode
        *   the exit code value which will be part of the new [[ExecutionOutput]] instance
        * @param standardOutput
        *   the standard output value which will be part of the new [[ExecutionOutput]] instance
        * @param standardError
        *   the standard error value which will be part of the new [[ExecutionOutput]] instance
        * @return
        *   a new [[ExecutionOutput]] trait instance with the given parameters
        */
    def apply(exitCode: Int, standardOutput: String, standardError: String): ExecutionOutput =
      ExecutionOutputImpl(exitCode, standardOutput, standardError)
  }
}
