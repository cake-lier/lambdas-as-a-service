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
package laas.master.ws.presentation

import java.util.UUID

import scala.util.Failure
import scala.util.Success

import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.Json
import io.circe.syntax.*

import laas.master.model.Executable.{ExecutableId, ExecutableType}
import laas.master.model.Execution.ExecutionArguments
import laas.master.model.User.DeployedExecutable
import laas.master.ws.presentation.Request.{Execute, Logout, Register}
import AnyOps.*

object Presentation {

  private given Decoder[Request.Login] = c =>
    for {
      _ <- c.downField("type").as[String].filterOrElse(_ === "login", DecodingFailure("The type field was not valid", c.history))
      u <- c.downField("username").as[String]
      p <- c.downField("password").as[String]
    } yield Request.Login(u, p)

  private given Decoder[Request.Logout] = c =>
    for {
      _ <- c.downField("type").as[String].filterOrElse(_ === "logout", DecodingFailure("The type field was not valid", c.history))
      u <- c.downField("username").as[String]
    } yield Request.Logout(u)

  private given Decoder[Request.Register] = c =>
    for {
      _ <- c
        .downField("type")
        .as[String]
        .filterOrElse(_ === "register", DecodingFailure("The type field was not valid", c.history))
      u <- c.downField("username").as[String]
      p <- c.downField("password").as[String]
    } yield Request.Register(u, p)

  private given Decoder[Request.Execute] = c =>
    for {
      _ <- c
        .downField("type")
        .as[String]
        .filterOrElse(_ === "execute", DecodingFailure("The type field was not valid", c.history))
      i <- c.downField("id").as[ExecutableId]
      a <- c.downField("args").as[String].map(_.split(';').toSeq)
    } yield Request.Execute(i, a)

  given Decoder[Request] = r =>
    r.as[Request.Login]
      .orElse[DecodingFailure, Request](r.as[Request.Logout])
      .orElse[DecodingFailure, Request](r.as[Request.Register])
      .orElse[DecodingFailure, Request](r.as[Request.Execute])

  private given Encoder[DeployedExecutable] = e =>
    Json.obj(
      "id" -> e.id.asJson,
      "name" -> e.name.asJson
    )

  private given Encoder[Response.LoginOutput] = r =>
    Json.obj(
      "type" -> "loginOutput".asJson,
      r.deployedExecutables match {
        case Failure(e) => "error" -> e.getMessage.asJson
        case Success(s) => "exec" -> s.asJson
      }
    )

  private given Encoder[Response.ExecuteOutput] = r =>
    Json.obj(
      "type" -> "executeOutput".asJson,
      "id" -> r.id.asJson,
      r.output match {
        case Failure(e) => "error" -> e.getMessage.asJson
        case Success(o) =>
          "output" -> Json.obj(
            "exitCode" -> o.exitCode.asJson,
            "stdout" -> o.standardOutput.asJson,
            "stderr" -> o.standardError.asJson
          )
      }
    )

  private given Encoder[Response.DeployOutput] = r =>
    Json.obj(
      "type" -> "deployOutput".asJson,
      r.id match {
        case Failure(e) => "error" -> e.getMessage.asJson
        case Success(i) => "id" -> i.asJson
      }
    )

  private given Encoder[Response.SendId] = r =>
    Json.obj(
      "type" -> "sendId".asJson,
      "id" -> r.id.asJson
    )

  given Encoder[Response] = {
    case r: Response.LoginOutput => r.asJson
    case r: Response.DeployOutput => r.asJson
    case r: Response.ExecuteOutput => r.asJson
    case r: Response.SendId => r.asJson
  }
}
