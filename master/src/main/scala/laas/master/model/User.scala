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
package laas.master.model

import java.util.UUID

import laas.master.model.Executable.ExecutableId

/** The object containing all domain entities related to the user. */
private[master] object User {

  /** An executable that has already been deployed.
    *
    * This trait represents an executable that has already been deployed. Being so, it has received a unique identifier and the
    * user that deployed it gave it a name.
    */
  trait DeployedExecutable {

    /** Returns the name of this executable, the one that the user gave to it while deploying it. */
    val name: String

    /** Returns the unique identifier assigned by the system to this executable. */
    val id: ExecutableId
  }

  /** Companion object to the [[DeployedExecutable]] trait, containing its factory method. */
  object DeployedExecutable {

    /* Implementation of the DeployedExecutable trait. */
    private case class DeployedExecutableImpl(name: String, id: ExecutableId) extends DeployedExecutable

      /** Factory method for creating a new instance of the [[DeployedExecutable]] trait, given the name that the user gave to it
        * while deploying it and the identifier that the system assigned it.
        *
        * @param name
        *   the name that the user gave to this executable while deploying it
        * @param id
        *   the identifier that the system gave to this executable
        * @return
        *   a new instance of the [[DeployedExecutable]] trait
        */
    def apply(name: String, id: ExecutableId): DeployedExecutable = DeployedExecutableImpl(name, id)
  }
}
