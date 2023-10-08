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
package laas.tuplespace.server.ws.service

import java.util.UUID

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

import laas.AnyOps.*
import laas.tuplespace.*
import laas.tuplespace.server.model.JsonTupleSpace
import laas.tuplespace.server.ws.presentation.request.*
import laas.tuplespace.server.ws.presentation.response.*

/** The actor representing the API of the tuple space, managing all operations, alongside the client management. */
private[server] object TupleSpaceApi {

  /* The main behavior of this actor. */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def main(
    ctx: ActorContext[TupleSpaceApiCommand],
    connections: Map[UUID, ActorRef[Response]],
    jsonTupleSpace: JsonTupleSpace
  ): Behavior[TupleSpaceApiCommand] = {
    given ExecutionContext = ctx.executionContext
    Behaviors.receiveMessage {
      case TupleSpaceApiCommand.Out(tuple, id) =>
        connections
          .get(id)
          .foreach(a => {
            jsonTupleSpace.out(tuple)
            a ! TupleResponse(tuple)
          })
        Behaviors.same
      case TupleSpaceApiCommand.In(template, id) =>
        connections
          .get(id)
          .foreach(a =>
            jsonTupleSpace
              .in(template, id)
              .onComplete(_.toOption.foreach(t => a ! TemplateTupleResponse(template, TemplateTupleResponseType.In, t)))
          )
        Behaviors.same
      case TupleSpaceApiCommand.Rd(template, id) =>
        connections
          .get(id)
          .foreach(a =>
            jsonTupleSpace
              .rd(template, id)
              .onComplete(_.toOption.foreach(t => a ! TemplateTupleResponse(template, TemplateTupleResponseType.Rd, t)))
          )
        Behaviors.same
      case TupleSpaceApiCommand.No(template, id) =>
        connections
          .get(id)
          .foreach(a =>
            jsonTupleSpace
              .no(template, id)
              .onComplete(_.toOption.foreach(t => a ! TemplateResponse(template)))
          )
        Behaviors.same
      case TupleSpaceApiCommand.Inp(template, id) =>
        connections
          .get(id)
          .foreach(_ ! TemplateMaybeTupleResponse(template, TemplateMaybeTupleResponseType.Inp, jsonTupleSpace.inp(template)))
        Behaviors.same
      case TupleSpaceApiCommand.Rdp(template, id) =>
        connections
          .get(id)
          .foreach(_ ! TemplateMaybeTupleResponse(template, TemplateMaybeTupleResponseType.Rdp, jsonTupleSpace.rdp(template)))
        Behaviors.same
      case TupleSpaceApiCommand.Nop(template, id) =>
        connections.get(id).foreach(_ ! TemplateBooleanResponse(template, jsonTupleSpace.nop(template)))
        Behaviors.same
      case TupleSpaceApiCommand.OutAll(tuples, id) =>
        connections
          .get(id)
          .foreach(a => {
            jsonTupleSpace.outAll(tuples: _*)
            a ! SeqTupleResponse(tuples)
          })
        Behaviors.same
      case TupleSpaceApiCommand.InAll(template, id) =>
        connections
          .get(id)
          .foreach(_ ! TemplateSeqTupleResponse(template, TemplateSeqTupleResponseType.InAll, jsonTupleSpace.inAll(template)))
        Behaviors.same
      case TupleSpaceApiCommand.RdAll(template, id) =>
        connections
          .get(id)
          .foreach(_ ! TemplateSeqTupleResponse(template, TemplateSeqTupleResponseType.RdAll, jsonTupleSpace.rdAll(template)))
        Behaviors.same
      case TupleSpaceApiCommand.Enter(replyTo, id) =>
        replyTo ! ConnectionSuccessResponse(id)
        main(ctx, connections + (id -> replyTo), jsonTupleSpace)
      case TupleSpaceApiCommand.MergeIds(oldId, id) =>
        connections
          .get(id)
          .fold(Behaviors.same)(a => {
            a ! MergeSuccessResponse(oldId)
            main(ctx, connections - id + (oldId -> a), jsonTupleSpace)
          })
      case TupleSpaceApiCommand.Exit(success, id) =>
        if (success) {
          jsonTupleSpace.remove(id)
        }
        main(ctx, connections - id, jsonTupleSpace)
    }
  }

  /** Creates a new tuple space API actor, given the root actor of its actor system to which signal its startup.
    *
    * @param root
    *   the root actor of the actor system of this actor
    * @return
    *   a new tuple space API actor
    */
  def apply(root: ActorRef[Unit]): Behavior[TupleSpaceApiCommand] = Behaviors.setup(ctx => {
    root ! ()
    main(ctx, Map.empty, JsonTupleSpace())
  })
}
