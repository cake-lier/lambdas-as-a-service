package io.github.cakelier
package laas.worker.model

import java.util.UUID

object Executable {

  /** The identifier which represents uniquely an executable file into the system. */
  type ExecutableId = UUID

  /** The type of execution that an executable file requires for being executed. */
  enum ExecutableType {

    /** The type for an executable which source code was written in the Java language. */
    case Java extends ExecutableType

    /** The type for an executable which source code was written in the Python language. */
    case Python extends ExecutableType
  }
}
