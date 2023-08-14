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

enum ServiceApiCommand {

  case RequestCommand(request: Request, replyTo: ActorRef[Response]) extends ServiceApiCommand

  case StorageLoginResponseCommand(username: String, result: Try[Seq[DeployedExecutable]], replyTo: ActorRef[Response])
    extends ServiceApiCommand

  case StorageRegisterResponseCommand(username: String, result: Try[Unit], replyTo: ActorRef[Response]) extends ServiceApiCommand

  case Deploy(id: ExecutableId, tpe: ExecutableType, fileName: String, websocketId: UUID) extends ServiceApiCommand

  case StartTimer(
    cfpId: CallForProposalId,
    tpe: ExecutableType,
    executableId: ExecutableId,
    username: String,
    fileName: String,
    replyTo: ActorRef[Response]
  ) extends ServiceApiCommand

  case GetProposals(
    cfpId: CallForProposalId,
    tpe: ExecutableType,
    executableId: ExecutableId,
    username: String,
    fileName: String,
    replyTo: ActorRef[Response]
  ) extends ServiceApiCommand

  case TryProposals(
    cfpId: CallForProposalId,
    executableId: ExecutableId,
    tpe: ExecutableType,
    proposals: Seq[(UUID, Int)],
    username: String,
    fileName: String,
    replyTo: ActorRef[Response]
  ) extends ServiceApiCommand

  case Open(actorRef: ActorRef[Response], id: UUID) extends ServiceApiCommand

  case Close(actorRef: ActorRef[Response], id: UUID) extends ServiceApiCommand
}
