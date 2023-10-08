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
package laas.master.ws.service

import java.util.UUID

import scala.util.Try

import akka.actor.typed.ActorRef

import laas.master.model.CallForProposal.CallForProposalId
import laas.master.model.Executable.{ExecutableId, ExecutableType}
import laas.master.model.User.DeployedExecutable
import laas.master.ws.presentation.{Request, Response}

/** The enum representing all the messages that can be sent to the [[ServiceApi]] agent.
  *
  * The messages that can be sent to the [[ServiceApi]] are three different kinds of command. The first kind is for forwarding the
  * request received by the [[ServiceController]] to the agent, so as to allow this last one to handle them properly and generate
  * the correct response. The second kind is for allowing the agent to be proactive, so they are commands that the actor which
  * implements the agent "self-sends" to achieve proactivity and reach its goal. The third kind are messages to handle the
  * connection status of a client, so they are used when a new connection has been established or it has been closed.
  */
private[service] enum ServiceApiCommand {

  /** The message used for forwarding any request coming from a web app websocket to the [[ServiceApi]] agent, in order to allow
    * this last one to handle them. The message comes also with the identifier of the connection on which the request arrived, so
    * as to let the [[ServiceApi]] retrieve connection state information.
    *
    * @constructor
    *   creates a new message given the [[Request]] to send to the agent and the identifier of the connection on which the request
    *   was sent
    */
  case RequestCommand(request: Request, id: UUID) extends ServiceApiCommand

    /** The message used for "self-sending" the result of getting the user state from the storage system when it is available. It
      * contains the username of the user who requested to get its state, the identifier for the connection on which the request
      * was received, the result, which can be succeeded or failed, and the actor to which send a message for responding.
      *
      * @constructor
      *   creates a new message given the username of the user who requested its state, the identifier of the connection on which
      *   the request was sent, the result of the operation which is a [[Try]] containing a [[Throwable]] if the operation failed,
      *   a [[Seq]] of [[DeployedExecutable]]s otherwise, and the [[ActorRef]] of the actor to be used for responding to the
      *   request
      */
  case UserStateResponseCommand(username: String, id: UUID, result: Try[Seq[DeployedExecutable]], replyTo: ActorRef[Response])
    extends ServiceApiCommand

    /** The message used for requesting the deployment of an executable. It contains the newly generated [[ExecutableId]] of the
      * executable to deploy, the [[ExecutableType]] of the executable to deploy, the name the user decided to associate to the
      * file and the id of the connection on which the request was made.
      *
      * @constructor
      *   creates a new message given the [[ExecutableId]] and the [[ExecutableType]] of the executable to deploy, the name the
      *   user decided to associate to it and the identifier of the connection on which the request was made
      */
  case Deploy(id: ExecutableId, tpe: ExecutableType, fileName: String, websocketId: UUID) extends ServiceApiCommand

    /** The message used for "self-sending" the notification to start the timer after publishing the call for proposals on the
      * tuple space, waiting for the proposals to arrive. It contains the [[CallForProposalId]] of the call for proposals, the
      * [[ExecutableId]] and the [[ExecutableType]] of the executable to deploy, the username of the user who requested the
      * deployment, the name that the user decided to associate to the file and the actor to which send a message for responding.
      *
      * @constructor
      *   creates a new message given the [[CallForProposalId]] of the launched call for proposal, the [[ExecutableType]] and the
      *   [[ExecutableId]] of the executable to deploy, the username of the user who requested the deployment, the name that they
      *   associated to the file and the [[ActorRef]] of the actor to be used for responding to the request
      */
  case StartTimer(
    cfpId: CallForProposalId,
    tpe: ExecutableType,
    executableId: ExecutableId,
    username: String,
    fileName: String,
    replyTo: ActorRef[Response]
  ) extends ServiceApiCommand

    /** The message used for "self-sending" the notification that the timer waiting for the proposals to arrive has elapsed and
      * the proposals can now be collected. It contains the [[CallForProposalId]] of the call for proposals, the [[ExecutableId]]
      * and the [[ExecutableType]] of the executable to deploy, the username of the user who requested the deployment, the name
      * that the user decided to associate to the file and the actor to which send a message for responding.
      *
      * @constructor
      *   creates a new message given the [[CallForProposalId]] of the launched call for proposal, the [[ExecutableType]] and the
      *   [[ExecutableId]] of the executable to deploy, the username of the user who requested the deployment, the name that they
      *   associated to the file and the [[ActorRef]] of the actor to be used for responding to the request
      */
  case GetProposals(
    cfpId: CallForProposalId,
    tpe: ExecutableType,
    executableId: ExecutableId,
    username: String,
    fileName: String,
    replyTo: ActorRef[Response]
  ) extends ServiceApiCommand

    /** The message used for "self-sending" the proposals to try for deploying the requested executable after having received them
      * from the tuple space or after trying another one and it failed. It contains the [[CallForProposalId]] of the call for
      * proposals, the [[ExecutableId]] and the [[ExecutableType]] of the executable to deploy, the proposals left to try, the
      * username of the user who requested the deployment, the name that the user decided to associate to the file and the actor
      * to which send a message for responding.
      *
      * @constructor
      *   creates a new message given the [[CallForProposalId]] of the launched call for proposal, the [[ExecutableId]] and the
      *   [[ExecutableType]] of the executable to deploy, the proposals left to try as a [[Seq]] of couples made of the worker
      *   identifier and the number of available slots for that worker, the username of the user who requested the deployment, the
      *   name that they associated to the file and the [[ActorRef]] of the actor to be used for responding to the request
      */
  case TryProposals(
    cfpId: CallForProposalId,
    executableId: ExecutableId,
    tpe: ExecutableType,
    proposals: Seq[(UUID, Int)],
    username: String,
    fileName: String,
    replyTo: ActorRef[Response]
  ) extends ServiceApiCommand

    /** The message sent from the [[ServiceController]] for notifying that a new connection from a web app was opened. It contains
      * the identifier of the connection and the actor to be used for responding to its requests.
      *
      * @constructor
      *   creates a new message given the [[ActorRef]] of the actor to be used for responding to the requests coming from the new
      *   connection and the identifier of the connection
      */
  case Open(actorRef: ActorRef[Response], id: UUID) extends ServiceApiCommand

    /** The message sent from the [[ServiceController]] for notifying that a connection from a web app was closed. It contains the
      * identifier of the connection.
      *
      * @constructor
      *   creates a new message given the identifier of the connection which has been closed
      */
  case Close(id: UUID) extends ServiceApiCommand
}
