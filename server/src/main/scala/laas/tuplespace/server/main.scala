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

import java.nio.file.Paths
import java.util.concurrent.ForkJoinPool

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor.ClassicActorSystemProvider
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/** The main entrypoint for the tuple space server. */
@main
def main(): Unit =
  ActorSystem[Unit](
    Behaviors.setup(ctx => {
      val config: Config = ConfigFactory.systemEnvironment()
      val tupleSpaceActor = ctx.spawn(TupleSpaceActor(ctx.self), name = "tuple-space")
      Behaviors.receiveMessage(_ => {
        given ClassicActorSystemProvider = ctx.system
        given ExecutionContext = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())

        val server: Future[Http.ServerBinding] =
          Http()
            .newServerAt("0.0.0.0", config.getInt("TUPLES_SPACE_PORT_NUMBER"))
            .bind(TupleSpaceRoute(config.getString("TUPLES_SPACE_SERVICE_PATH"), tupleSpaceActor, ctx.system))
        ctx.system.whenTerminated.onComplete(_ => server.map(_.unbind()))
        Behaviors.empty
      })
    }),
    name = "tuple-space-root"
  )
