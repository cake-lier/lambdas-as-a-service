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
package laas.master.ws

import java.util.concurrent.ForkJoinPool

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.getquill.JdbcContextConfig

import laas.master.ws.service.{ServiceApi, ServiceController, ServiceStorage}
import laas.tuplespace.client.JsonTupleSpace

@main
def main(): Unit = {
  given ExecutionContext = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
  val config: Config = ConfigFactory.systemEnvironment()
  JsonTupleSpace(config.getString("MASTER_TS_URI")).foreach(s => {
    ActorSystem[Unit](
      Behaviors.setup(ctx => {
        given ActorSystem[Nothing] = ctx.system
        val server =
          Http()
            .newServerAt("0.0.0.0", config.getInt("MASTER_PORT_NUMBER"))
            .bind(
              ServiceController(
                ctx.spawn(
                  ServiceApi(
                    ServiceStorage(
                      JdbcContextConfig(
                        ConfigFactory
                          .parseMap(
                            Map(
                              "dataSourceClassName" -> "org.postgresql.ds.PGSimpleDataSource",
                              "dataSource.user" -> config.getString("DB_USERNAME"),
                              "dataSource.password" -> config.getString("DB_PASSWORD"),
                              "dataSource.databaseName" -> config.getString("DB_NAME"),
                              "dataSource.portNumber" -> config.getInt("DB_PORT_NUMBER"),
                              "dataSource.serverName" -> config.getString("DB_HOSTNAME"),
                              "connectionTimeout" -> config.getInt("DB_TIMEOUT")
                            ).asJava
                          )
                      ).dataSource
                    ),
                    s
                  ),
                  "master"
                )
              )
            )
        ctx.system.whenTerminated.onComplete(_ => server.flatMap(_.unbind()).flatMap(_ => s.close()))
        Behaviors.empty
      }),
      name = "root-master"
    )
  })
}
