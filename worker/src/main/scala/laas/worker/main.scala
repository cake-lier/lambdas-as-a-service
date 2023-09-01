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
package laas.worker

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.ForkJoinPool
import java.util.stream.Collectors

import scala.concurrent.ExecutionContext

import akka.actor.ClassicActorSystemProvider
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.stream.scaladsl.FileIO
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import laas.worker.agents.{RootActorCommand, WorkerAgent}
import laas.tuplespace.client.*
import laas.worker.model.Executable.ExecutableType

@main
def main(): Unit = {
  given ExecutionContext = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
  val config: Config = ConfigFactory.systemEnvironment()
  JsonTupleSpace(config.getString("WORKER_TS_URI"), config.getInt("WORKER_BUFFER")).foreach(s => {
    ActorSystem[RootActorCommand](
      Behaviors.setup(ctx => {
        given ClassicActorSystemProvider = ctx.system
        val client = Http()
        ctx.spawn(
          WorkerAgent(
            ctx.self,
            UUID.fromString(config.getString("WORKER_ID")),
            s,
            (e, t) =>
              client
                .singleRequest(Get(s"${config.getString("WORKER_HTTP_CLIENT_URI")}/${e.toString}"))
                .flatMap(
                  _.entity
                    .dataBytes
                    .runWith(FileIO.toPath(Paths.get("executables", s"${e.toString}.${t.extension}")))
                    .map(_ => ())
                ),
            config.getString("WORKER_ACCEPTED_EXECUTABLES").split(';').toSeq.map(ExecutableType.valueOf),
            config.getInt("WORKER_AVAILABLE_SLOTS")
          ),
          "worker-" + config.getString("WORKER_ID")
        )
        ctx.system.whenTerminated.onComplete(_ => s.close())
        Behaviors.receiveMessage[RootActorCommand] {
          case RootActorCommand.WorkerUp(true) => Behaviors.empty
          case RootActorCommand.WorkerUp(false) => Behaviors.stopped
        }
      }),
      name = "root-worker-" + config.getString("WORKER_ID")
    )
  })
}
