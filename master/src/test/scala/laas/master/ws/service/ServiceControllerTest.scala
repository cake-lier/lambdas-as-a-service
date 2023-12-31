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
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.testkit.WSProbe
import akka.http.scaladsl.testkit.WSTestRequestBuilding.WS
import akka.stream.scaladsl.FileIO
import org.apache.commons.io.file.PathUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import laas.master.model.Executable.ExecutableType
import laas.master.model.Execution.ExecutionOutput
import laas.master.model.User.DeployedExecutable
import laas.master.ws.presentation.{Request, Response}

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
class ServiceControllerTest extends AnyFunSpec with ScalatestRouteTest with BeforeAndAfterAll {

  private val testKit = ActorTestKit()
  private val apiProbe = testKit.createTestProbe[ServiceApiCommand]()
  private given ActorSystem[Nothing] = testKit.system

  private val controller = ServiceController(apiProbe.ref)

  override protected def afterAll(): Unit = testKit.shutdownTestKit()

  private val username = "mario"
  private val password = "password"
  private val executableId = UUID.randomUUID()
  private val executionArgs = Seq("out", "err")

  describe("A service controller") {
    describe("when a new websocket is opened and then closed") {
      it("should notify the api") {
        val wsProbe: WSProbe = WSProbe()
        WS("/service/ws", wsProbe.flow) ~> controller ~> check {
          val openMessage: ServiceApiCommand.Open = apiProbe.expectMessageType[ServiceApiCommand.Open]
          wsProbe.sendCompletion()
          apiProbe.expectMessage(ServiceApiCommand.Close(openMessage.id))
        }
      }
    }

    describe("when a request is sent") {
      it("should forward it to the api") {
        val wsProbe: WSProbe = WSProbe()
        WS("/service/ws", wsProbe.flow) ~> controller ~> check {
          val openMessage = apiProbe.expectMessageType[ServiceApiCommand.Open]
          wsProbe.sendMessage(s"{\"type\":\"login\",\"username\":\"$username\",\"password\":\"$password\"}")
          apiProbe.expectMessage(ServiceApiCommand.RequestCommand(Request.Login(username, password), openMessage.id))
          wsProbe.sendMessage(s"{\"type\":\"register\",\"username\":\"$username\",\"password\":\"$password\"}")
          apiProbe.expectMessage(ServiceApiCommand.RequestCommand(Request.Register(username, password), openMessage.id))
          wsProbe.sendMessage("{\"type\":\"logout\"}")
          apiProbe.expectMessage(ServiceApiCommand.RequestCommand(Request.Logout, openMessage.id))
          wsProbe.sendMessage(s"{\"type\":\"execute\",\"id\":\"${executableId.toString}\",\"args\":\"out;err\"}")
          apiProbe.expectMessage(
            ServiceApiCommand.RequestCommand(Request.Execute(executableId, executionArgs), openMessage.id)
          )
          wsProbe.sendMessage(s"{\"type\":\"userState\",\"id\":\"${executableId.toString}\"}")
          apiProbe.expectMessage(
            ServiceApiCommand.RequestCommand(Request.UserState(executableId), openMessage.id)
          )
          wsProbe.sendCompletion()
          apiProbe.expectMessage(ServiceApiCommand.Close(openMessage.id))
        }
      }
    }

    describe("when a deploy request is sent") {
      it("should forward it to the api and the file should be downloadable") {
        val wsProbe: WSProbe = WSProbe()
        WS("/service/ws", wsProbe.flow) ~> controller ~> check {
          val openMessage = apiProbe.expectMessageType[ServiceApiCommand.Open]
          val originalFilePath = Paths.get("master", "src", "test", "resources", "exec.jar")
          val name = "test"
          val multipartForm =
            Multipart.FormData(
              Multipart
                .FormData
                .BodyPart
                .fromPath(
                  "file",
                  ContentTypes.`text/plain(UTF-8)`,
                  originalFilePath
                ),
              Multipart
                .FormData
                .BodyPart
                .Strict(
                  "name",
                  HttpEntity(ContentTypes.`text/plain(UTF-8)`, name)
                ),
              Multipart
                .FormData
                .BodyPart
                .Strict(
                  "id",
                  HttpEntity(ContentTypes.`text/plain(UTF-8)`, openMessage.id.toString)
                )
            )
          Post("/service/deploy", multipartForm) ~> controller ~> check {
            response.status shouldBe StatusCodes.OK
            val deployMessage = apiProbe.expectMessageType[ServiceApiCommand.Deploy]
            deployMessage.tpe shouldBe ExecutableType.Java
            deployMessage.fileName shouldBe name
            deployMessage.websocketId shouldBe openMessage.id
            val uploadedFilePath = Paths.get(deployMessage.id.toString)
            PathUtils.fileContentEquals(uploadedFilePath, originalFilePath) shouldBe true
            Get(s"/service/files/${deployMessage.id}") ~> controller ~> check {
              response.status shouldBe StatusCodes.OK
              val downloadedFilePath = Paths.get("master", "copy.jar")
              Await.result(responseEntity.dataBytes.runWith(FileIO.toPath(downloadedFilePath)), 30.seconds)
              PathUtils.fileContentEquals(uploadedFilePath, downloadedFilePath) shouldBe true
              Files.delete(uploadedFilePath)
              Files.delete(downloadedFilePath)
            }
          }
          wsProbe.sendCompletion()
          apiProbe.expectMessage(ServiceApiCommand.Close(openMessage.id))
        }
      }
    }

    describe("when a response is generated") {
      it("should send it to the opened websocket") {
        val wsProbe: WSProbe = WSProbe()
        WS("/service/ws", wsProbe.flow) ~> controller ~> check {
          val openMessage = apiProbe.expectMessageType[ServiceApiCommand.Open]
          val generatedId = UUID.randomUUID()
          val name = "test"
          val error = "The user must be logged in to perform this operation."
          openMessage.actorRef ! Response.SendId(generatedId)
          wsProbe.expectMessage(s"{\"type\":\"sendId\",\"id\":\"${generatedId.toString}\"}")
          openMessage.actorRef ! Response.UserStateOutput(Success(Seq(DeployedExecutable(name, generatedId))))
          wsProbe.expectMessage(
            s"{\"type\":\"userStateOutput\",\"exec\":[{\"id\":\"${generatedId.toString}\",\"name\":\"$name\"}]}"
          )
          openMessage.actorRef ! Response.UserStateOutput(Failure(Exception(error)))
          wsProbe.expectMessage(s"{\"type\":\"userStateOutput\",\"error\":\"$error\"}")
          openMessage.actorRef ! Response.ExecuteOutput(generatedId, Success(ExecutionOutput(0, "out\n", "err\n")))
          wsProbe.expectMessage(
            s"{\"type\":\"executeOutput\",\"id\":\"${generatedId.toString}\",\"output\":{\"exitCode\":0," +
            "\"stdout\":\"out\\n\",\"stderr\":\"err\\n\"}}"
          )
          openMessage.actorRef ! Response.ExecuteOutput(generatedId, Failure(Exception(error)))
          wsProbe.expectMessage(s"{\"type\":\"executeOutput\",\"id\":\"${generatedId.toString}\",\"error\":\"$error\"}")
          openMessage.actorRef ! Response.DeployOutput(Success(generatedId))
          wsProbe.expectMessage(s"{\"type\":\"deployOutput\",\"id\":\"${generatedId.toString}\"}")
          openMessage.actorRef ! Response.DeployOutput(Failure(Exception(error)))
          wsProbe.expectMessage(s"{\"type\":\"deployOutput\",\"error\":\"$error\"}")
          wsProbe.sendCompletion()
          apiProbe.expectMessage(ServiceApiCommand.Close(openMessage.id))
        }
      }
    }
  }
}
