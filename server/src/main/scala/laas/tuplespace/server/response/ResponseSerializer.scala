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
package laas.tuplespace.server.response

import io.circe.Encoder
import io.circe.Json
import io.circe.syntax.*

import laas.tuplespace.*
import laas.tuplespace.JsonSerializable.given

/** This object contains all serializers for the [[Response]] sub-types. */
private[server] object ResponseSerializer {

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
