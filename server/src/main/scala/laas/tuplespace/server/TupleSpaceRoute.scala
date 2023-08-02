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
import laas.tuplespace.server.response.*
import laas.tuplespace.server.response.ResponseSerializer.given
import laas.tuplespace.server.request.*
import laas.tuplespace.server.request.RequestDeserializer.given

/** The routes of the webservice which handles the websocket connections to the tuple space server. */
@SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
private[server] object TupleSpaceRoute {

  /** Creates a new [[Route]] object representing the routes of the webservice which is the server of the tuple space. The URL
    * path to the webservice must be given, along with the actor handling the requests and the [[ActorSystem]] on which executing
    * all the message handling operations.
    *
    * @param servicePath
    *   the URL path on which the tuple space webservice is located
    * @param tupleSpaceActor
    *   the tuple space actor which will handle all the requests
    * @param actorSystem
    *   the [[ActorSystem]] on which all operations for message handling will be executed
    * @return
    *   a new [[Route]] instance object
    */
  def apply(servicePath: String, tupleSpaceActor: ActorRef[TupleSpaceActorCommand], actorSystem: ActorSystem[Nothing]): Route = {
    given ClassicActorSystem = actorSystem.classicSystem
    given ExecutionContextExecutor = actorSystem.dispatchers.lookup(DispatcherSelector.default())
    path(servicePath) {
      handleWebSocketMessages {
        val (actorRef: ActorRef[Response], source: Source[Response, NotUsed]) =
          ActorSource
            .actorRef[Response](PartialFunction.empty, PartialFunction.empty, 1, OverflowStrategy.dropHead)
            .preMaterialize()
        tupleSpaceActor ! TupleSpaceActorCommand.Enter(actorRef)

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
                case r: MergeRequest => TupleSpaceActorCommand.MergeIds(r.oldClientId, actorRef)
                case r: TupleRequest => TupleSpaceActorCommand.Out(r.content, actorRef)
                case r: TemplateRequest =>
                  r.tpe match {
                    case TemplateRequestType.In => TupleSpaceActorCommand.In(r.content, actorRef)
                    case TemplateRequestType.Rd => TupleSpaceActorCommand.Rd(r.content, actorRef)
                    case TemplateRequestType.No => TupleSpaceActorCommand.No(r.content, actorRef)
                    case TemplateRequestType.InAll => TupleSpaceActorCommand.InAll(r.content, actorRef)
                    case TemplateRequestType.RdAll => TupleSpaceActorCommand.RdAll(r.content, actorRef)
                    case TemplateRequestType.Inp => TupleSpaceActorCommand.Inp(r.content, actorRef)
                    case TemplateRequestType.Rdp => TupleSpaceActorCommand.Rdp(r.content, actorRef)
                    case TemplateRequestType.Nop => TupleSpaceActorCommand.Nop(r.content, actorRef)
                  }
                case r: SeqTupleRequest => TupleSpaceActorCommand.OutAll(r.content, actorRef)
              }
            } yield r).map(Source.single[TupleSpaceActorCommand]).getOrElse(Source.empty[TupleSpaceActorCommand])
          )
          .via(
            Flow.fromSinkAndSourceCoupled(
              ActorSink.actorRef[TupleSpaceActorCommand](
                tupleSpaceActor,
                TupleSpaceActorCommand.Exit(success = true, actorRef),
                _ => TupleSpaceActorCommand.Exit(success = false, actorRef)
              ),
              source
            )
          )
          .map(r => TextMessage(r.asJson.noSpaces))
      }
    }
  }
}
