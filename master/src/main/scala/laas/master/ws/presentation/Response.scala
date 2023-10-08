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
package laas.master.ws.presentation

import java.util.UUID

import scala.util.Try

import laas.master.model.Executable.ExecutableId
import laas.master.model.Execution.ExecutionOutput
import laas.master.model.User.DeployedExecutable

/** The enum representing all different responses that the [[laas.master.ws.service.ServiceApi]] agent can send through a
  * websocket connection.
  *
  * The possible responses that can be sent are the initial one, the one for letting the web app know what is the unique
  * identifier associated to the connection it opened, and the "output" ones. The "output" responses are responses sent after a
  * matching request has been previously sent to the [[laas.master.ws.service.ServiceApi]].
  */
private[ws] enum Response {

  /** The response sent after the connection with the web app was established. It contains the identifier associated to the
    * websocket, so to the connection it established. This is sent in order for the web app to send it back when making a "deploy"
    * request via HTTP, so as to let the [[laas.master.ws.service.ServiceApi]] know from which client it comes. Furthermore, it is
    * used when the user logs in and decides to refresh the page or close it and return to it at a later moment. By sending the
    * old connection identifier, the [[laas.master.ws.service.ServiceApi]] can know that the user has already logged in and it
    * doesn't need to do it twice.
    *
    * @constructor
    *   creates a new response given the identifier of the connection that it has just been established
    */
  case SendId(id: UUID) extends Response

    /** The response sent after the user requested to register, to log in or to access its current state in general. It contains
      * the user state, which is made by all [[DeployedExecutable]]s that the user has previously deployed, if the operation which
      * requested it has completed with success, a [[Throwable]] representing the reason for the failure otherwise.
      *
      * @constructor
      *   creates a new response given a [[Try]] representing the success of the operation for which it has been generated. If the
      *   operation completed with success, it will contain a [[Seq]] of all [[DeployedExecutable]]s previously deployed by a user
      */
  case UserStateOutput(deployedExecutables: Try[Seq[DeployedExecutable]]) extends Response

    /** The response sent after the user requested to execute an executable file. It contains the [[ExecutableId]] of the
      * executable that was requested to execute and the [[ExecutionOutput]] of the execution, if it completed with success.
      * Otherwise, it will contain a [[Throwable]] with the reason of the error.
      *
      * @constructor
      *   creates a new response given the [[ExecutableId]] of the executable that was requested to execute and a [[Try]]
      *   containing a [[Throwable]] if the execution failed and the [[ExecutionOutput]] if not
      */
  case ExecuteOutput(id: ExecutableId, output: Try[ExecutionOutput]) extends Response

    /** The response sent after the user requested to deploy an executable file. It contains the [[ExecutableId]] of the deployed
      * executable if the operation completed with success, a [[Throwable]] with the reason of the error otherwise.
      *
      * @constructor
      *   creates a new response given a [[Try]] containing a [[Throwable]] if the deployment operation did not succeed, the
      *   [[ExecutableId]] of the deployed executable otherwise
      */
  case DeployOutput(id: Try[ExecutableId]) extends Response
}
