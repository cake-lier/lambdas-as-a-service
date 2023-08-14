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

import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.ForkJoinPool

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem as ClassicActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.stream.scaladsl.FileIO

import laas.worker.agents.{RootActorCommand, WorkerAgent}
import laas.tuplespace.client.*
import laas.worker.model.Executable.ExecutableType

@main
def main(
  id: String,
  tupleSpaceUri: String,
  httpClientUri: String,
  acceptedExecutableTypes: String,
  slotsAvailable: Int
): Unit = {
  given ExecutionContext = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
  JsonTupleSpace(tupleSpaceUri).foreach(s => {
    given ClassicActorSystem = ClassicActorSystem()
    val client = Http()
    ActorSystem[RootActorCommand](
      Behaviors.setup(ctx => {
        ctx.spawn(
          WorkerAgent(
            ctx.self,
            UUID.fromString(id),
            s,
            e =>
              client
                .singleRequest(Get(s"$httpClientUri/${e.toString}"))
                .flatMap(_.entity.dataBytes.runWith(FileIO.toPath(Paths.get("executables", e.toString))).map(_ => ())),
            acceptedExecutableTypes.split(';').toSeq.map(ExecutableType.valueOf),
            slotsAvailable
          ),
          "worker-" + id
        )
        Behaviors.receiveMessage[RootActorCommand] {
          case RootActorCommand.WorkerUp(true) =>
            println("it worked!")
            Behaviors.empty
          case RootActorCommand.WorkerUp(false) =>
            println("it doesn't work")
            Behaviors.stopped
        }
      }),
      "root-worker-" + id
    )
  })
}
