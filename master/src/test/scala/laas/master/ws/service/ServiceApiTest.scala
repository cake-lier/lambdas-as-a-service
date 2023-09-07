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

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*
import scala.util.Failure
import scala.util.Success

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.testkit.typed.scaladsl.TestProbe
import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import com.typesafe.config.ConfigFactory
import io.getquill.JdbcContextConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.TryValues.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

import laas.master.model.Executable.ExecutableType
import laas.master.model.Execution.ExecutionOutput
import laas.master.model.User.DeployedExecutable
import laas.master.model.{Performative, User}
import laas.master.ws.presentation.{Request, Response}
import laas.tuplespace.*
import laas.tuplespace.client.JsonTupleSpace

@SuppressWarnings(
  Array(
    "org.wartremover.warts.GlobalExecutionContext",
    "org.wartremover.warts.ToString",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.IterableOps",
    "scalafix:DisableSyntax.var"
  )
)
class ServiceApiTest extends AnyFunSpec with BeforeAndAfterAll with TestContainersForAll {

  override type Containers = PostgreSQLContainer and GenericContainer

  private val timeout: FiniteDuration = 30.seconds
  private val databaseName: String = "test"
  private val databaseUsername: String = "test"
  private val databasePassword: String = "test"

  override def startContainers(): Containers = {
    val storageContainer = PostgreSQLContainer
      .Def(
        dockerImageName = DockerImageName.parse("postgres:15.4"),
        databaseName = databaseName,
        username = databaseUsername,
        password = databasePassword,
        commonJdbcParams = CommonParams(timeout, timeout, Some("init.sql"))
      )
      .start()
    val tupleSpaceContainer = GenericContainer
      .Def(
        "matteocastellucci3/laas-ts-server:latest",
        exposedPorts = Seq(80),
        waitStrategy = Wait.forListeningPort()
      )
      .start()
    storageContainer and tupleSpaceContainer
  }

  private given ExecutionContext = scala.concurrent.ExecutionContext.global

  private val testKit: ActorTestKit = ActorTestKit()
  private val responseActorProbe: TestProbe[Response] = testKit.createTestProbe[Response]()
  private var storage: Option[ServiceStorage] = None
  private var tupleSpaceFactory: Option[() => JsonTupleSpace] = None
  private val username = "mario"
  private val password = "password"
  private val executableName = "test"

  override def afterContainersStart(containers: and[PostgreSQLContainer, GenericContainer]): Unit = {
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
                "dataSource.portNumber" -> containers.head.container.getFirstMappedPort.intValue,
                "dataSource.serverName" -> "localhost",
                "connectionTimeout" -> timeout.length.intValue * 1000
              ).asJava
            )
        ).dataSource
      )
    )
    tupleSpaceFactory = Some(() =>
      Await.result(
        JsonTupleSpace("ws://localhost:" + containers.tail.container.getFirstMappedPort.toString + "/tuplespace"),
        timeout
      )
    )
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    testKit.shutdownTestKit()
  }

  describe("A master agent") {
    describe("when receives a websocket open message") {
      it("should send to the websocket the newly generated id") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when a new user registers to the system") {
      it("should add them to the system") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        master ! ServiceApiCommand.RequestCommand(Request.Register(username, password), websocketId)
        responseActorProbe.expectMessage(Response.UserStateOutput(Success(Seq.empty)))
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when an already registered user is added to the system") {
      it("should not add them to the system and return an error") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        master ! ServiceApiCommand.RequestCommand(Request.Register(username, "password2"), websocketId)
        val response = responseActorProbe.expectMessageType[Response.UserStateOutput]
        response.deployedExecutables.failure
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when an already registered user logs in the system") {
      it("should log them in the system") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        responseActorProbe.expectMessage(Response.UserStateOutput(Success(Seq.empty)))
        master ! ServiceApiCommand.RequestCommand(Request.Logout, websocketId)
        responseActorProbe.expectNoMessage()
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when a new executable is deployed in the system but the user didn't log in") {
      it("should not deploy it and return an error") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        val executableId = UUID.randomUUID()
        Files.copy(
          Paths.get("master", "src", "test", "resources", "exec.jar"),
          Paths.get(executableId.toString)
        )
        master ! ServiceApiCommand.Deploy(executableId, ExecutableType.Java, executableName, websocketId)
        val result: Response.DeployOutput = responseActorProbe.expectMessageType[Response.DeployOutput]
        result.id.failure.exception.getMessage shouldBe "You need to be logged in to perform this operation."
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        responseActorProbe.expectMessage(Response.UserStateOutput(Success(Seq.empty)))
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when a new executable is deployed in the system but no worker accepts the deployment") {
      it("should not deploy it and return an error") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        val executableId = UUID.randomUUID()
        Files.copy(
          Paths.get("master", "src", "test", "resources", "exec.jar"),
          Paths.get(executableId.toString)
        )
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        responseActorProbe.expectMessage(Response.UserStateOutput(Success(Seq.empty)))
        master ! ServiceApiCommand.Deploy(executableId, ExecutableType.Java, executableName, websocketId)
        val tupleSpace = tupleSpaceFactory.getOrElse(fail())()
        Await.result(
          for {
            cfp <- tupleSpace
              .rd(
                complete(
                  Performative.Cfp.name,
                  string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
                  ExecutableType.Java.toString
                )
              )
            cfpId = cfp.elem(1).getOrElse(fail()).toString
            _ <- tupleSpace
              .out(
                Performative.Refuse.name #:
                cfpId #:
                JsonNil
              )
            _ <- tupleSpace
              .no(
                complete(
                  Performative.Cfp.name,
                  cfpId,
                  ExecutableType.Java.toString
                )
              )
          } yield (),
          timeout
        )
        val result: Response.DeployOutput = responseActorProbe.expectMessageType[Response.DeployOutput]
        result.id.failure.exception.getMessage shouldBe "The executable cannot be allocated."
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        responseActorProbe.expectMessage(Response.UserStateOutput(Success(Seq.empty)))
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when a new executable is deployed in the system but the worker fails") {
      it("should not deploy it and return an error") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        val executableId = UUID.randomUUID()
        Files.copy(
          Paths.get("master", "src", "test", "resources", "exec.jar"),
          Paths.get(executableId.toString)
        )
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        responseActorProbe.expectMessage(Response.UserStateOutput(Success(Seq.empty)))
        master ! ServiceApiCommand.Deploy(executableId, ExecutableType.Java, executableName, websocketId)
        val workerId = UUID.randomUUID()
        val tupleSpace = tupleSpaceFactory.getOrElse(fail())()
        Await.result(
          for {
            cfp <- tupleSpace
              .rd(
                complete(
                  Performative.Cfp.name,
                  string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
                  ExecutableType.Java.toString
                )
              )
            cfpId = cfp.elem(1).getOrElse(fail()).toString
            _ <- tupleSpace
              .out(
                Performative.Propose.name #:
                cfpId #:
                workerId.toString #:
                1 #:
                JsonNil
              )
            _ <- tupleSpace
              .in(
                complete(
                  Performative.AcceptProposal.name,
                  cfpId,
                  workerId.toString,
                  string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
                  ExecutableType.Java.toString
                )
              )
            _ <- tupleSpace
              .out(
                Performative.Failure.name #:
                "cfp" #:
                cfpId #:
                JsonNil
              )
          } yield (),
          timeout
        )
        val result: Response.DeployOutput = responseActorProbe.expectMessageType[Response.DeployOutput]
        result.id.failure.exception.getMessage shouldBe "The executable cannot be allocated."
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        responseActorProbe.expectMessage(Response.UserStateOutput(Success(Seq.empty)))
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when a new executable is deployed in the system and a worker succeeds") {
      it("should deploy it and return the id of the executable") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        val executableId = UUID.randomUUID()
        Files.copy(
          Paths.get("master", "src", "test", "resources", "exec.jar"),
          Paths.get(executableId.toString)
        )
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        responseActorProbe.expectMessage(Response.UserStateOutput(Success(Seq.empty)))
        master ! ServiceApiCommand.Deploy(executableId, ExecutableType.Java, executableName, websocketId)
        val firstWorkerId = UUID.randomUUID()
        val secondWorkerId = UUID.randomUUID()
        val tupleSpace = tupleSpaceFactory.getOrElse(fail())()
        Await.result(
          for {
            cfp <- tupleSpace
              .rd(
                complete(
                  Performative.Cfp.name,
                  string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
                  ExecutableType.Java.toString
                )
              )
            cfpId = cfp.elem(1).getOrElse(fail()).toString
            _ <- tupleSpace
              .out(
                Performative.Propose.name #:
                cfpId #:
                secondWorkerId.toString #:
                1 #:
                JsonNil
              )
            _ <- tupleSpace
              .out(
                Performative.Propose.name #:
                cfpId #:
                firstWorkerId.toString #:
                2 #:
                JsonNil
              )
            _ <- tupleSpace
              .in(
                complete(
                  Performative.AcceptProposal.name,
                  cfpId,
                  firstWorkerId.toString,
                  string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
                  ExecutableType.Java.toString
                )
              )
            _ <- tupleSpace
              .out(
                Performative.InformDone.name #:
                "cfp" #:
                cfpId #:
                JsonNil
              )
            _ <- tupleSpace
              .in(
                complete(
                  Performative.RejectProposal.name,
                  cfpId,
                  secondWorkerId.toString
                )
              )
          } yield (),
          timeout
        )
        val result: Response.DeployOutput = responseActorProbe.expectMessageType[Response.DeployOutput]
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        responseActorProbe.expectMessage(
          Response.UserStateOutput(Success(Seq(DeployedExecutable(executableName, result.id.success.value))))
        )
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when a new executable is deployed in the system and a first worker fails, but a second succeeds") {
      it("should deploy it and return the id of the executable") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        val executableId = UUID.randomUUID()
        Files.copy(
          Paths.get("master", "src", "test", "resources", "exec.jar"),
          Paths.get(executableId.toString)
        )
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        val previousExecutableId = responseActorProbe.expectMessageType[Response.UserStateOutput].deployedExecutables.success.value
        master ! ServiceApiCommand.Deploy(executableId, ExecutableType.Java, executableName, websocketId)
        val firstWorkerId = UUID.randomUUID()
        val secondWorkerId = UUID.randomUUID()
        val tupleSpace = tupleSpaceFactory.getOrElse(fail())()
        Await.result(
          for {
            cfp <- tupleSpace
              .rd(
                complete(
                  Performative.Cfp.name,
                  string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
                  ExecutableType.Java.toString
                )
              )
            cfpId = cfp.elem(1).getOrElse(fail()).toString
            _ <- tupleSpace
              .out(
                Performative.Propose.name #:
                cfpId #:
                secondWorkerId.toString #:
                1 #:
                JsonNil
              )
            _ <- tupleSpace
              .out(
                Performative.Propose.name #:
                cfpId #:
                firstWorkerId.toString #:
                2 #:
                JsonNil
              )
            _ <- tupleSpace
              .in(
                complete(
                  Performative.AcceptProposal.name,
                  cfpId,
                  firstWorkerId.toString,
                  string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
                  ExecutableType.Java.toString
                )
              )
            _ <- tupleSpace
              .out(
                Performative.Failure.name #:
                "cfp" #:
                cfpId #:
                JsonNil
              )
            _ <- tupleSpace
              .in(
                complete(
                  Performative.AcceptProposal.name,
                  cfpId,
                  secondWorkerId.toString,
                  string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
                  ExecutableType.Java.toString
                )
              )
            _ <- tupleSpace
              .out(
                Performative.InformDone.name #:
                "cfp" #:
                cfpId #:
                JsonNil
              )
          } yield (),
          timeout
        )
        val result = responseActorProbe.expectMessageType[Response.DeployOutput]
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        val UserStateOutput = responseActorProbe.expectMessageType[Response.UserStateOutput]
        UserStateOutput.deployedExecutables.success.value shouldBe (
          previousExecutableId :+ DeployedExecutable(executableName, result.id.success.value)
        )
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when an executable is to be executed but the user didn't log in") {
      it("should not execute it and return an error") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        val executableId = UUID.randomUUID()
        master ! ServiceApiCommand.RequestCommand(Request.Execute(executableId, Seq("out", "err")), websocketId)
        val result = responseActorProbe.expectMessageType[Response.ExecuteOutput]
        result.output.failure.exception.getMessage shouldBe "You need to be logged in to perform this operation."
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when an executable is to be executed but is not associated to the logged in user") {
      it("should not execute it and return an error") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        val deployedExecutables = responseActorProbe.expectMessageType[Response.UserStateOutput].deployedExecutables.success.value
        master ! ServiceApiCommand.RequestCommand(Request.Register("luigi", "password"), websocketId)
        responseActorProbe.expectMessage(Response.UserStateOutput(Success(Seq.empty)))
        master ! ServiceApiCommand.RequestCommand(
          Request.Execute(deployedExecutables.head.id, Seq("out", "err")),
          websocketId
        )
        val result = responseActorProbe.expectMessageType[Response.ExecuteOutput]
        result.output.failure.exception.getMessage shouldBe "The executable id provided was not found."
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }

    describe("when an executable is to be executed") {
      it("should execute it and return the result") {
        val master = testKit.spawn(ServiceApi(storage.getOrElse(fail()), tupleSpaceFactory.getOrElse(fail())()))
        val websocketId = UUID.randomUUID()
        master ! ServiceApiCommand.Open(responseActorProbe.ref, websocketId)
        responseActorProbe.expectMessage(Response.SendId(websocketId))
        master ! ServiceApiCommand.RequestCommand(Request.Login(username, password), websocketId)
        val executableId = responseActorProbe.expectMessageType[Response.UserStateOutput].deployedExecutables.success.value.head.id
        val executableArgs = Seq("out", "err")
        val executionOutput = ExecutionOutput(0, "out\n", "err\n")
        master ! ServiceApiCommand.RequestCommand(Request.Execute(executableId, executableArgs), websocketId)
        val tupleSpace = tupleSpaceFactory.getOrElse(fail())()
        Await.result(
          for {
            request <- tupleSpace
              .in(
                complete(
                  Performative.Request.name,
                  "execute",
                  string matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$".r,
                  executableId.toString,
                  String.join(";", executableArgs: _*)
                )
              )
            executionId = request.elem(2).getOrElse(fail()).toString
            _ <- tupleSpace
              .out(
                Performative.InformResult.name #:
                "execute" #:
                executionId #:
                executionOutput.exitCode #:
                executionOutput.standardOutput #:
                executionOutput.standardError #:
                JsonNil
              )
          } yield (),
          timeout
        )
        val result = responseActorProbe.expectMessage(Response.ExecuteOutput(executableId, Success(executionOutput)))
        master ! ServiceApiCommand.Close(websocketId)
        responseActorProbe.expectNoMessage()
        testKit.stop(master)
      }
    }
  }
}
