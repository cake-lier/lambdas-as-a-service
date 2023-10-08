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

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

import akka.NotUsed
import akka.actor.ActorSystem as ClassicActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.DispatcherSelector
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSink
import akka.stream.typed.scaladsl.ActorSource
import io.circe.parser.*
import io.circe.syntax.*

import laas.tuplespace.*
import laas.tuplespace.server.ws.presentation.Presentation.given
import laas.tuplespace.server.ws.presentation.request.*
import laas.tuplespace.server.ws.presentation.response.*

/** The routes of the webservice which handle the websocket connections to the tuple space server. */
@SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
private[server] object TupleSpaceController {

  /** Creates a new [[Route]] object representing the routes of the webservice which is the server of the tuple space. The URL
    * path to the webservice must be given, along with the actor handling the requests and the [[ActorSystem]] on which executing
    * all the message handling operations.
    *
    * @param servicePath
    *   the URL path on which the tuple space webservice is located
    * @param tupleSpaceActor
    *   the tuple space actor API which will handle all the requests
    * @param actorSystem
    *   the [[ActorSystem]] on which all operations for message handling will be executed
    * @return
    *   a new [[Route]] instance object
    */
  def apply(servicePath: String, tupleSpaceActor: ActorRef[TupleSpaceApiCommand], actorSystem: ActorSystem[Nothing]): Route = {
    given ClassicActorSystem = actorSystem.classicSystem
    given ExecutionContextExecutor = actorSystem.dispatchers.lookup(DispatcherSelector.default())
    path(servicePath) {
      handleWebSocketMessages {
        val id: UUID = UUID.randomUUID()
        Flow[Message]
          .mapAsync(1) {
            case m: TextMessage.Strict => Future.successful(m)
            case m: TextMessage.Streamed => m.textStream.runFold("")(_ + _).map(TextMessage.apply)
            case m: BinaryMessage => m.dataStream.runWith(Sink.ignore).map(_ => null)
          }
          .flatMapConcat(t =>
            (for {
              j <- parse(t.text).toOption
              m <- j.as[Request].toOption
              r = m match {
                case r: MergeRequest => TupleSpaceApiCommand.MergeIds(r.oldClientId, id)
                case r: TupleRequest => TupleSpaceApiCommand.Out(r.content, id)
                case r: TemplateRequest =>
                  r.tpe match {
                    case TemplateRequestType.In => TupleSpaceApiCommand.In(r.content, id)
                    case TemplateRequestType.Rd => TupleSpaceApiCommand.Rd(r.content, id)
                    case TemplateRequestType.No => TupleSpaceApiCommand.No(r.content, id)
                    case TemplateRequestType.InAll => TupleSpaceApiCommand.InAll(r.content, id)
                    case TemplateRequestType.RdAll => TupleSpaceApiCommand.RdAll(r.content, id)
                    case TemplateRequestType.Inp => TupleSpaceApiCommand.Inp(r.content, id)
                    case TemplateRequestType.Rdp => TupleSpaceApiCommand.Rdp(r.content, id)
                    case TemplateRequestType.Nop => TupleSpaceApiCommand.Nop(r.content, id)
                  }
                case r: SeqTupleRequest => TupleSpaceApiCommand.OutAll(r.content, id)
              }
            } yield r).map(Source.single[TupleSpaceApiCommand]).getOrElse(Source.empty[TupleSpaceApiCommand])
          )
          .via(
            Flow.fromSinkAndSourceCoupled(
              ActorSink.actorRef[TupleSpaceApiCommand](
                tupleSpaceActor,
                TupleSpaceApiCommand.Exit(success = true, id),
                _ => TupleSpaceApiCommand.Exit(success = false, id)
              ),
              ActorSource
                .actorRef[Response](PartialFunction.empty, PartialFunction.empty, 100, OverflowStrategy.dropHead)
                .mapMaterializedValue(a => tupleSpaceActor ! TupleSpaceApiCommand.Enter(a, id))
            )
          )
          .map(r => TextMessage(r.asJson.noSpaces))
      }
    }
  }
}
