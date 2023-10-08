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
package laas.tuplespace.server.ws.presentation

import laas.AnyOps.*
import laas.tuplespace.*
import laas.tuplespace.JsonSerializable.given
import laas.tuplespace.server.ws.presentation.request.*
import laas.tuplespace.server.ws.presentation.request.Request.*
import laas.tuplespace.server.ws.presentation.response.*
import laas.tuplespace.server.ws.presentation.response.Response.*

import io.circe.*
import io.circe.syntax.*

/** This object contains all serializers and deserializers for [[Request]]s and [[Response]]s. */
private[ws] object Presentation {

  /* The Decoder given instance for the TupleRequest trait. */
  private given Decoder[TupleRequest] = c =>
    for {
      content <- c.downField("content").as[JsonTuple]
      - <- c
        .downField("type")
        .as[String]
        .filterOrElse(
          _ === "out",
          DecodingFailure(
            DecodingFailure.Reason.CustomReason("The value for the type field was not valid"),
            c.downField("type")
          )
        )
    } yield TupleRequest(content)

  /* The Decoder given instance for the SeqTupleRequest trait. */
  private given Decoder[SeqTupleRequest] = c =>
    for {
      content <- c.downField("content").as[Seq[JsonTuple]]
      - <- c
        .downField("type")
        .as[String]
        .filterOrElse(
          _ === "outAll",
          DecodingFailure(
            DecodingFailure.Reason.CustomReason("The value for the type field was not valid"),
            c.downField("type")
          )
        )
    } yield SeqTupleRequest(content)

  /* The Decoder given instance for the TemplateRequest trait. */
  private given Decoder[TemplateRequest] = c =>
    for {
      content <- c.downField("content").as[JsonTemplate]
      tpe <- c.downField("type").as[String].flatMap {
        case "in" => Right[DecodingFailure, TemplateRequestType](TemplateRequestType.In)
        case "rd" => Right[DecodingFailure, TemplateRequestType](TemplateRequestType.Rd)
        case "no" => Right[DecodingFailure, TemplateRequestType](TemplateRequestType.No)
        case "inp" => Right[DecodingFailure, TemplateRequestType](TemplateRequestType.Inp)
        case "rdp" => Right[DecodingFailure, TemplateRequestType](TemplateRequestType.Rdp)
        case "nop" => Right[DecodingFailure, TemplateRequestType](TemplateRequestType.Nop)
        case "inAll" => Right[DecodingFailure, TemplateRequestType](TemplateRequestType.InAll)
        case "rdAll" => Right[DecodingFailure, TemplateRequestType](TemplateRequestType.RdAll)
        case _ =>
          Left[DecodingFailure, TemplateRequestType](
            DecodingFailure(
              DecodingFailure.Reason.CustomReason("The value for the type field was not valid"),
              c.downField("type")
            )
          )
      }
    } yield TemplateRequest(content, tpe)

  /* The Decoder given instance for the MergeRequest trait. */
  private given Decoder[MergeRequest] = Decoder.forProduct1("oldClientId")(MergeRequest.apply)

  /** The [[Decoder]] given instance for the general [[Request]] trait, working for all of its sub-types. */
  given Decoder[Request] = r =>
    r.as[TupleRequest]
      .orElse[DecodingFailure, Request](r.as[SeqTupleRequest])
      .orElse[DecodingFailure, Request](r.as[MergeRequest])
      .orElse[DecodingFailure, Request](r.as[TemplateRequest])

  /* The Encoder given instance for the TupleResponse trait. */
  private given Encoder[TupleResponse] = r =>
    Json.obj(
      "request" -> r.request.asJson,
      "type" -> "out".asJson,
      "content" -> ().asJson
    )

  /* The Encoder given instance for the SeqTupleResponse trait. */
  private given Encoder[SeqTupleResponse] = r =>
    Json.obj(
      "request" -> r.request.asJson,
      "type" -> "outAll".asJson,
      "content" -> ().asJson
    )

  /* The Encoder given instance for the TemplateTupleResponse trait. */
  private given Encoder[TemplateTupleResponse] = r =>
    Json.obj(
      "request" -> r.request.asJson,
      "type" -> (r.tpe match {
        case TemplateTupleResponseType.In => "in"
        case TemplateTupleResponseType.Rd => "rd"
      }).asJson,
      "content" -> r.content.asJson
    )

  /* The Encoder given instance for the TemplateMaybeTupleResponse trait. */
  private given Encoder[TemplateMaybeTupleResponse] = r =>
    Json.obj(
      "request" -> r.request.asJson,
      "type" -> (r.tpe match {
        case TemplateMaybeTupleResponseType.Inp => "inp"
        case TemplateMaybeTupleResponseType.Rdp => "rdp"
      }).asJson,
      "content" -> r.content.asJson
    )

  /* The Encoder given instance for the TemplateSeqTupleResponse trait. */
  private given Encoder[TemplateSeqTupleResponse] = r =>
    Json.obj(
      "request" -> r.request.asJson,
      "type" -> (r.tpe match {
        case TemplateSeqTupleResponseType.InAll => "inAll"
        case TemplateSeqTupleResponseType.RdAll => "rdAll"
      }).asJson,
      "content" -> r.content.asJson
    )

  /* The Encoder given instance for the TemplateResponse trait. */
  private given Encoder[TemplateResponse] = r =>
    Json.obj(
      "request" -> r.request.asJson,
      "type" -> "no".asJson,
      "content" -> ().asJson
    )

  /* The Encoder given instance for the TemplateBooleanResponse trait. */
  private given Encoder[TemplateBooleanResponse] = r =>
    Json.obj(
      "request" -> r.request.asJson,
      "type" -> "nop".asJson,
      "content" -> r.content.asJson
    )

  /* The Encoder given instance for the ConnectionSuccessResponse trait. */
  private given Encoder[ConnectionSuccessResponse] = r =>
    Json.obj(
      "clientId" -> r.clientId.asJson
    )

  /* The Encoder given instance for the ConnectionSuccessResponse trait. */
  private given Encoder[MergeSuccessResponse] = r => Json.obj("oldClientId" -> r.oldClientId.asJson)

  /** The [[Encoder]] given instance for the general [[Response]] trait, working for all of its sub-types. */
  given Encoder[Response] = {
    case r: TupleResponse => r.asJson
    case r: SeqTupleResponse => r.asJson
    case r: TemplateTupleResponse => r.asJson
    case r: TemplateResponse => r.asJson
    case r: TemplateBooleanResponse => r.asJson
    case r: TemplateMaybeTupleResponse => r.asJson
    case r: TemplateSeqTupleResponse => r.asJson
    case r: ConnectionSuccessResponse => r.asJson
    case r: MergeSuccessResponse => r.asJson
  }
}
