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
package laas.tuplespace.server

import java.util.UUID

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

import AnyOps.*
import laas.tuplespace.*
import laas.tuplespace.server.request.*
import laas.tuplespace.server.response.*

/** The actor representing the handler of the tuple space, managing all operations, alongside the client management and the id
  * assignment.
  */
private[server] object TupleSpaceActor {

  /* The main behavior of this actor. */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def main(
    ctx: ActorContext[TupleSpaceActorCommand],
    connections: Map[UUID, ActorRef[Response]],
    jsonTupleSpace: JsonTupleSpace
  ): Behavior[TupleSpaceActorCommand] = {
    given ExecutionContext = ctx.executionContext
    Behaviors.receiveMessage {
      case TupleSpaceActorCommand.Out(tuple, id) =>
        connections
          .get(id)
          .foreach(a => {
            jsonTupleSpace.out(tuple)
            a ! TupleResponse(tuple)
          })
        Behaviors.same
      case TupleSpaceActorCommand.In(template, id) =>
        connections
          .get(id)
          .foreach(a =>
            jsonTupleSpace
              .in(template, id)
              .onComplete(_.toOption.foreach(t => a ! TemplateTupleResponse(template, TemplateTupleResponseType.In, t)))
          )
        Behaviors.same
      case TupleSpaceActorCommand.Rd(template, id) =>
        connections
          .get(id)
          .foreach(a =>
            jsonTupleSpace
              .rd(template, id)
              .onComplete(_.toOption.foreach(t => a ! TemplateTupleResponse(template, TemplateTupleResponseType.Rd, t)))
          )
        Behaviors.same
      case TupleSpaceActorCommand.No(template, id) =>
        connections
          .get(id)
          .foreach(a =>
            jsonTupleSpace
              .no(template, id)
              .onComplete(_.toOption.foreach(t => a ! TemplateResponse(template)))
          )
        Behaviors.same
      case TupleSpaceActorCommand.Inp(template, id) =>
        connections
          .get(id)
          .foreach(_ ! TemplateMaybeTupleResponse(template, TemplateMaybeTupleResponseType.Inp, jsonTupleSpace.inp(template)))
        Behaviors.same
      case TupleSpaceActorCommand.Rdp(template, id) =>
        connections
          .get(id)
          .foreach(_ ! TemplateMaybeTupleResponse(template, TemplateMaybeTupleResponseType.Rdp, jsonTupleSpace.rdp(template)))
        Behaviors.same
      case TupleSpaceActorCommand.Nop(template, id) =>
        connections.get(id).foreach(_ ! TemplateBooleanResponse(template, jsonTupleSpace.nop(template)))
        Behaviors.same
      case TupleSpaceActorCommand.OutAll(tuples, id) =>
        connections
          .get(id)
          .foreach(a => {
            jsonTupleSpace.outAll(tuples: _*)
            a ! SeqTupleResponse(tuples)
          })
        Behaviors.same
      case TupleSpaceActorCommand.InAll(template, id) =>
        connections
          .get(id)
          .foreach(_ ! TemplateSeqTupleResponse(template, TemplateSeqTupleResponseType.InAll, jsonTupleSpace.inAll(template)))
        Behaviors.same
      case TupleSpaceActorCommand.RdAll(template, id) =>
        connections
          .get(id)
          .foreach(_ ! TemplateSeqTupleResponse(template, TemplateSeqTupleResponseType.RdAll, jsonTupleSpace.rdAll(template)))
        Behaviors.same
      case TupleSpaceActorCommand.Enter(replyTo, id) =>
        replyTo ! ConnectionSuccessResponse(id)
        main(ctx, connections + (id -> replyTo), jsonTupleSpace)
      case TupleSpaceActorCommand.MergeIds(oldId, id) =>
        connections
          .get(id)
          .fold(Behaviors.same)(a => {
            a ! MergeSuccessResponse(oldId)
            main(ctx, connections - id + (oldId -> a), jsonTupleSpace)
          })
      case TupleSpaceActorCommand.Exit(success, id) =>
        if (success) {
          jsonTupleSpace.remove(id)
        }
        main(ctx, connections - id, jsonTupleSpace)
    }
  }

  /** Creates a new tuple space actor, given the root actor of its actor system to which signal its startup.
    *
    * @param root
    *   the root actor of the actor system of this actor
    * @return
    *   a new tuple space actor
    */
  def apply(root: ActorRef[Unit]): Behavior[TupleSpaceActorCommand] = Behaviors.setup(ctx => {
    root ! ()
    main(ctx, Map.empty, JsonTupleSpace())
  })
}
