package io.github.cakelier
package laas.worker.model

import java.util.UUID

object Execution {

  type ExecutionId = UUID

  /** The arguments passed as input to the executable file to be executed. */
  type ExecutionArguments = Seq[String]

  /**
   * The output of an execution, it is made by the exit code of the process which executed the executable file, what was
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

    /* Implementation of the Output trait. */
    private case class ExecutionOutputImpl(exitCode: Int, standardOutput: String, standardError: String) extends ExecutionOutput

    def apply(exitCode: Int, standardOutput: String, standardError: String): ExecutionOutput =
      ExecutionOutputImpl(exitCode, standardOutput, standardError)
  }
}
