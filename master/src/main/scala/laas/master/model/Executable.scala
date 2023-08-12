package io.github.cakelier
package laas.master.model

import java.util.UUID

import AnyOps.*

object Executable {

  /** The identifier which represents uniquely an executable file into the system. */
  type ExecutableId = UUID

  /** The type of execution that an executable file requires for being executed. */
  enum ExecutableType(val extension: String) {

    /** The type for an executable which source code was written in the Java language. */
    case Java extends ExecutableType("jar")

      /** The type for an executable which source code was written in the Python language. */
    case Python extends ExecutableType("py")
  }

  object ExecutableType {

    /** Returns the correct [[ExecutableType]] given the file extension associated to the type itself.
      *
      * @param extension
      *   the file extension for which getting the correct [[ExecutableType]]
      * @return
      *   the correct [[ExecutableType]] given the file extension associated to the type itself
      */
    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    def getByExtension(extension: String): ExecutableType = ExecutableType.values.find(_.extension === extension).get
  }
}
