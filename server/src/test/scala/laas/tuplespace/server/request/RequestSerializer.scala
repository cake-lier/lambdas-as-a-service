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
package laas.tuplespace.server.request

import io.circe.Encoder
import io.circe.Json
import io.circe.syntax.*
import AnyOps.*
import laas.tuplespace.*
import laas.tuplespace.JsonSerializable.given

import io.github.cakelier.laas.tuplespace.server.ws.presentation.request.{Request, TemplateRequestType}

private[server] object RequestSerializer {

  private given Encoder[TupleRequest] = r =>
    Json.obj(
      "content" -> r.content.asJson,
      "type" -> "out".asJson
    )

  private given Encoder[SeqTupleRequest] = r =>
    Json.obj(
      "content" -> r.content.asJson,
      "type" -> "outAll".asJson
    )

  private given Encoder[TemplateRequest] = r =>
    Json.obj(
      "content" -> r.content.asJson,
      "type" -> (r.tpe match {
        case TemplateRequestType.In => "in"
        case TemplateRequestType.Rd => "rd"
        case TemplateRequestType.No => "no"
        case TemplateRequestType.Inp => "inp"
        case TemplateRequestType.Rdp => "rdp"
        case TemplateRequestType.Nop => "nop"
        case TemplateRequestType.InAll => "inAll"
        case TemplateRequestType.RdAll => "rdAll"
      }).asJson
    )

  private given Encoder[MergeRequest] = r =>
    Json.obj(
      "oldClientId" -> r.oldClientId.asJson
    )

  given Encoder[Request] = {
    case r: TupleRequest => r.asJson
    case r: TemplateRequest => r.asJson
    case r: SeqTupleRequest => r.asJson
    case r: MergeRequest => r.asJson
  }
}
