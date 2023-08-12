package io.github.cakelier
package laas.master.model

import java.util.UUID

import laas.master.model.Executable.ExecutableId

object User {

  trait DeployedExecutable {

    val name: String

    val id: ExecutableId
  }

  object DeployedExecutable {

    private case class DeployedExecutableImpl(name: String, id: ExecutableId) extends DeployedExecutable

    def apply(name: String, id: ExecutableId): DeployedExecutable = DeployedExecutableImpl(name, id)
  }
}
