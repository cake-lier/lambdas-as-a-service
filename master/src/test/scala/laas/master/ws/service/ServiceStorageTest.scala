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

import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*

import com.dimafeng.testcontainers
import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import io.getquill.JdbcContextConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.OptionValues.*
import org.scalatest.TryValues.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.testcontainers.utility.DockerImageName

import laas.master.model.User.DeployedExecutable

@SuppressWarnings(
  Array(
    "org.wartremover.warts.GlobalExecutionContext",
    "org.wartremover.warts.Var",
    "scalafix:DisableSyntax.var"
  )
)
class ServiceStorageTest extends AnyFunSpec with BeforeAndAfterAll with TestContainerForAll {

  private given ExecutionContext = scala.concurrent.ExecutionContext.global

  private val timeout: FiniteDuration = 30.seconds
  private val databaseName: String = "test"
  private val databaseUsername: String = "test"
  private val databasePassword: String = "test"

  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:15.4"),
    databaseName = databaseName,
    username = databaseUsername,
    password = databasePassword,
    commonJdbcParams = CommonParams(timeout, timeout, Some("init.sql"))
  )

  private var storage: Option[ServiceStorage] = None

  override def afterContainersStart(containers: containerDef.Container): Unit = {
    storage = Some(
      ServiceStorage(
        JdbcContextConfig(
          ConfigFactory
            .parseMap(
              Map(
                "dataSourceClassName" -> "org.postgresql.ds.PGSimpleDataSource",
                "dataSource.user" -> databaseUsername,
                "dataSource.password" -> databasePassword,
                "dataSource.databaseName" -> databaseName,
                "dataSource.portNumber" -> containers.container.getFirstMappedPort.intValue,
                "dataSource.serverName" -> "localhost",
                "connectionTimeout" -> timeout.length.intValue * 1000
              ).asJava
            )
        ).dataSource
      )
    )
  }

  private val username: String = "mario"
  private val password: String = "password"
  private val executableId: UUID = UUID.randomUUID()
  private val executableName: String = "test file"

  describe("A service storage") {
    describe("when registering a user") {
      it("should insert into itself") {
        Await.result(storage.getOrElse(fail()).register(username, password), timeout)
        Await.result(storage.getOrElse(fail()).login(username, password), timeout) shouldBe true
      }
    }

    describe("when registering a user with the same username") {
      it("should fail the operation") {
        Await.ready(storage.getOrElse(fail()).register(username, password), timeout).value.value.failure
      }
    }

    describe("when logging in a non existing user or a user with a wrong password") {
      it("should complete with an error") {
        Await.result(storage.getOrElse(fail()).login(username, "wrongPassword"), timeout) shouldBe false
        Await.result(storage.getOrElse(fail()).login("luigi", password), timeout) shouldBe false
      }
    }

    describe("when getting the executables for a newly registered user or a user that never registered") {
      it("should return an empty Seq") {
        Await.result(storage.getOrElse(fail()).findByUsername(username), timeout) shouldBe empty
        Await.result(storage.getOrElse(fail()).isExecutableOfUser(username, executableId), timeout) shouldBe false
        Await.result(storage.getOrElse(fail()).findByUsername("luigi"), timeout) shouldBe empty
        Await.result(storage.getOrElse(fail()).isExecutableOfUser("luigi", executableId), timeout) shouldBe false
      }
    }

    describe("when adding an executable to a user") {
      it("should associate it to the user") {
        Await.result(storage.getOrElse(fail()).addExecutableToUser(username, executableId, executableName), timeout)
        Await.result(storage.getOrElse(fail()).findByUsername(username), timeout) shouldBe Seq(
          DeployedExecutable(executableName, executableId)
        )
        Await.result(storage.getOrElse(fail()).isExecutableOfUser(username, executableId), timeout) shouldBe true
      }
    }

    describe("when adding an executable to a user with the same id of another") {
      it("should fail the operation") {
        Await
          .ready(storage.getOrElse(fail()).addExecutableToUser(username, executableId, executableName), timeout)
          .value
          .value
          .failure
      }
    }
  }
}
