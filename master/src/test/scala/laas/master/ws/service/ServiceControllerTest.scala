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
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import laas.master.model.Executable.ExecutableType
import laas.master.model.Execution.ExecutionOutput
import laas.master.model.User.DeployedExecutable
import laas.master.ws.presentation.{Request, Response}

import akka.stream.scaladsl.FileIO
import org.apache.commons.io.FileUtils

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

  describe("A controller") {
    describe("when a new websocket is opened and then closed") {
      it("should notify the api") {
        val wsProbe: WSProbe = WSProbe()
        WS("/service", wsProbe.flow) ~> controller ~> check {
          val openMessage: ServiceApiCommand.Open = apiProbe.expectMessageType[ServiceApiCommand.Open]
          wsProbe.sendCompletion()
          apiProbe.expectMessage(ServiceApiCommand.Close(openMessage.actorRef, openMessage.id))
        }
      }
    }

    describe("when a request is sent") {
      it("should forward it to the api") {
        val wsProbe: WSProbe = WSProbe()
        WS("/service", wsProbe.flow) ~> controller ~> check {
          val openMessage = apiProbe.expectMessageType[ServiceApiCommand.Open]
          wsProbe.sendMessage(s"{\"type\":\"login\",\"username\":\"$username\",\"password\":\"$password\"}")
          apiProbe.expectMessage(ServiceApiCommand.RequestCommand(Request.Login(username, password), openMessage.actorRef))
          wsProbe.sendMessage(s"{\"type\":\"register\",\"username\":\"$username\",\"password\":\"$password\"}")
          apiProbe.expectMessage(ServiceApiCommand.RequestCommand(Request.Register(username, password), openMessage.actorRef))
          wsProbe.sendMessage(s"{\"type\":\"logout\",\"username\":\"$username\"}")
          apiProbe.expectMessage(ServiceApiCommand.RequestCommand(Request.Logout(username), openMessage.actorRef))
          wsProbe.sendMessage(s"{\"type\":\"execute\",\"id\":\"${executableId.toString}\",\"args\":\"out;err\"}")
          apiProbe.expectMessage(
            ServiceApiCommand.RequestCommand(Request.Execute(executableId, executionArgs), openMessage.actorRef)
          )
          wsProbe.sendCompletion()
          apiProbe.expectMessage(ServiceApiCommand.Close(openMessage.actorRef, openMessage.id))
        }
      }
    }

    describe("when a deploy request is sent") {
      it("should forward it to the api and the file should be downloadable") {
        val wsProbe: WSProbe = WSProbe()
        WS("/service", wsProbe.flow) ~> controller ~> check {
          val openMessage = apiProbe.expectMessageType[ServiceApiCommand.Open]
          val name = "test"
          val originalFilePath = Paths.get("master", "src", "test", "resources", "exec.jar")
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
          Post("/deploy", multipartForm) ~> controller ~> check {
            response.status shouldBe StatusCodes.OK
            val deployMessage = apiProbe.expectMessageType[ServiceApiCommand.Deploy]
            deployMessage.tpe shouldBe ExecutableType.Java
            deployMessage.fileName shouldBe name
            deployMessage.websocketId shouldBe openMessage.id
            Get(s"/files/${deployMessage.id}") ~> controller ~> check {
              response.status shouldBe StatusCodes.OK
              val copyPath = Paths.get("copy.jar")
              responseEntity.dataBytes.runWith(FileIO.toPath(copyPath))
              FileUtils.contentEquals(originalFilePath.toFile, copyPath.toFile) shouldBe true
              Files.delete(Paths.get(deployMessage.id.toString))
              Files.delete(copyPath)
            }
          }
          wsProbe.sendCompletion()
          apiProbe.expectMessage(ServiceApiCommand.Close(openMessage.actorRef, openMessage.id))
        }
      }
    }

    describe("when a response is generated") {
      it("should send it to the opened websocket") {
        val wsProbe: WSProbe = WSProbe()
        WS("/service", wsProbe.flow) ~> controller ~> check {
          val openMessage = apiProbe.expectMessageType[ServiceApiCommand.Open]
          val generatedId = UUID.randomUUID()
          val name = "test"
          val error = "The user must be logged in to perform this operation."
          openMessage.actorRef ! Response.SendId(generatedId)
          wsProbe.expectMessage(s"{\"type\":\"sendId\",\"id\":\"${generatedId.toString}\"}")
          openMessage.actorRef ! Response.LoginOutput(Success(Seq(DeployedExecutable(name, generatedId))))
          wsProbe.expectMessage(
            s"{\"type\":\"loginOutput\",\"exec\":[{\"id\":\"${generatedId.toString}\",\"name\":\"$name\"}]}"
          )
          openMessage.actorRef ! Response.LoginOutput(Failure(Exception(error)))
          wsProbe.expectMessage(s"{\"type\":\"loginOutput\",\"error\":\"$error\"}")
          openMessage.actorRef ! Response.ExecuteOutput(generatedId, Success(ExecutionOutput(0, "out\n", "err\n")))
          wsProbe.expectMessage(
            s"{\"type\":\"executeOutput\",\"id\":\"${generatedId.toString}\",\"output\":{\"exitCode\":0," +
            "\"stdout\":\"out\\n\",\"stderr\":\"err\\n\"}}"
          )
          openMessage.actorRef ! Response.ExecuteOutput(generatedId, Failure(Exception(error)))
          wsProbe.expectMessage(s"{\"type\":\"executeOutput\",\"id\":\"${generatedId.toString}\",\"error\":\"$error\"}")
          wsProbe.sendCompletion()
          apiProbe.expectMessage(ServiceApiCommand.Close(openMessage.actorRef, openMessage.id))
        }
      }
    }
  }
}
