package io.github.cakelier
package laas.master.ws

import laas.master.ws.service.{ServiceApi, ServiceController, ServiceStorage}

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.JdbcContextConfig

import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext

@main
def main(): Unit = {
  given ExecutionContext = ExecutionContext.fromExecutor(ForkJoinPool().commonPool())
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
        ctx.system.whenTerminated.onComplete(_ => server.map(_.unbind()))
        Behaviors.empty
      }),
      name = "root-master"
    )
  })
}
