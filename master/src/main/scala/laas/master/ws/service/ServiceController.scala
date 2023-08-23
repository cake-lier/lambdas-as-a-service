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

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.NotUsed
import akka.actor.ActorSystem as ClassicActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.DispatcherSelector
import akka.actor.typed.scaladsl.AskPattern.*
import akka.http.scaladsl.model.StatusCodes
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
import akka.util.Timeout
import io.circe.parser.*
import io.circe.syntax.EncoderOps

import laas.master.model.Execution.{ExecutionArguments, ExecutionOutput}
import laas.master.model.Executable.ExecutableType
import laas.master.ws.presentation.{Request, Response}
import laas.master.ws.presentation.Presentation.given

object ServiceController {

  private given Timeout = 30.seconds

  @SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
  def apply(api: ActorRef[ServiceApiCommand])(using actorSystem: ActorSystem[Nothing]): Route = {
    given ClassicActorSystem = actorSystem.classicSystem
    given ExecutionContextExecutor = actorSystem.dispatchers.lookup(DispatcherSelector.blocking())

    concat(
      path("service") {
        handleWebSocketMessages {
          val (actorRef: ActorRef[Response], source: Source[Response, NotUsed]) =
            ActorSource
              .actorRef[Response](PartialFunction.empty, PartialFunction.empty, 1, OverflowStrategy.dropHead)
              .preMaterialize()
          val id = UUID.randomUUID()
          api ! ServiceApiCommand.Open(actorRef, id)
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
              } yield ServiceApiCommand.RequestCommand(m, actorRef))
                .map(Source.single[ServiceApiCommand])
                .getOrElse(Source.empty[ServiceApiCommand])
            )
            .via(
              Flow.fromSinkAndSourceCoupled(
                ActorSink.actorRef[ServiceApiCommand](
                  api,
                  ServiceApiCommand.Close(actorRef, id),
                  _ => ServiceApiCommand.Close(actorRef, id)
                ),
                source
              )
            )
            .map(r => TextMessage(r.asJson.noSpaces))
        }
      },
      path("deploy") {
        formFields("name", "id".as[UUID]) { (name, id) =>
          val executableId = UUID.randomUUID()
          storeUploadedFile(
            "file",
            i => Files.createFile(Paths.get(executableId.toString)).toFile
          ) { (fileInfo, file) =>
            api ! ServiceApiCommand.Deploy(
              executableId,
              ExecutableType.getByExtension(fileInfo.fileName.split('.')(1)),
              name,
              id
            )
            complete(StatusCodes.OK)
          }
        }
      },
      path("files" / JavaUUID) { id =>
        getFromFile(id.toString)
      }
    )
  }
}
