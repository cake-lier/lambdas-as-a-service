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
package laas.tuplespace

import scala.util.Try

import io.circe.Json

/** A type-class representing the operations that need to be supported by an object which can be serialized into a JSON-formatted
  * string.
  *
  * An object which can be serialized into a string using the JSON formatting needs to specify how to do so using a specific
  * operation, which takes the object as input and returns a JSON-formatted string as output. Whether or not this function returns
  * valid JSON, or JSON at all, is left to the right implementation of the function for the corresponding type of the object.
  *
  * @tparam A
  *   the type of the object that can be serialized in a JSON-formatted way
  */
trait JsonSerializable[-A] {

  /** Serializes an object into a string using the JSON format specification.
    *
    * @param e
    *   the object to serialize
    * @return
    *   the serialized version of the object, in JSON format
    */
  def serialize(e: A): String
}

/** Companion object to the [[JsonSerializable]] type-class, containing its interface and its implementations. */
@SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
object JsonSerializable {

  import io.circe.syntax.EncoderOps
  import io.circe.parser.parse

  /** The interface of the [[JsonSerializable]] type-class. */
  extension [A](i: A)(using JsonSerializable[A]) {

    /** Serializes this instance into a string using the JSON format specification.
      *
      * @return
      *   the serialized version of this instance, in JSON format
      */
    def serialize: String = implicitly[JsonSerializable[A]].serialize(i)
  }

  import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
  import io.circe.syntax.EncoderOps

  /** The implementation of the [[io.circe.Encoder]] type-class for the [[JsonElement]] type. */
  given Encoder[JsonElement] = {
    case v: Int => v.asJson
    case v: Long => v.asJson
    case v: Double => v.asJson
    case v: Float => v.asJson
    case v: Boolean => v.asJson
    case v: String => v.asJson
    case v: JsonTuple => v.asJson
    case null => Json.Null
  }

  /** The implementation of the [[io.circe.Decoder]] type-class for the [[JsonElement]] type. */
  given Decoder[JsonElement] = c =>
    if (c.value.isNull)
      Right[DecodingFailure, JsonElement](value = null)
    else
      c.as[Int]
        .orElse[DecodingFailure, JsonElement](c.as[Long])
        .orElse[DecodingFailure, JsonElement](c.as[Double])
        .orElse[DecodingFailure, JsonElement](c.as[Float])
        .orElse[DecodingFailure, JsonElement](c.as[Boolean])
        .orElse[DecodingFailure, JsonElement](c.as[JsonTuple])
        .orElse[DecodingFailure, JsonElement](c.as[String])

        /** The implementation of the [[JsonSerializable]] type-class for the [[JsonElement]] type. */
  given JsonSerializable[JsonElement] with {

    override def serialize(e: JsonElement): String = e.asJson.noSpaces
  }

  /** The implementation of the [[io.circe.Encoder]] type-class for the [[JsonTuple]] type. */
  given Encoder[JsonTuple] = t => t.toSeq.asJson

    /** The implementation of the [[io.circe.Decoder]] type-class for the [[JsonTuple]] type. */
  given Decoder[JsonTuple] = _.as[Seq[JsonElement]].map(JsonTuple.fromSeq)

  /** The implementation of the [[JsonSerializable]] type-class for the [[JsonTuple]] type. */
  given jsonTupleJsonSerializable[T <: JsonTuple]: JsonSerializable[T] with {

    override def serialize(t: T): String = (t: JsonTuple).asJson.noSpaces
  }

  private given Encoder[JsonAnyOfTemplate] = t =>
    Json.obj(
      "templates" -> t.templates.asJson,
      "type" -> Json.fromString("AnyOfTemplate")
    )

  private given Decoder[JsonAnyOfTemplate] = Decoder.forProduct1("templates")(JsonAnyOfTemplate.apply)

  private given Encoder[JsonAllOfTemplate] = t =>
    Json.obj(
      "templates" -> t.templates.asJson,
      "type" -> Json.fromString("AllOfTemplate")
    )

  private given Decoder[JsonAllOfTemplate] = Decoder.forProduct1("templates")(JsonAllOfTemplate.apply)

  private given Encoder[JsonOneOfTemplate] = t =>
    Json.obj(
      "templates" -> t.templates.asJson,
      "type" -> Json.fromString("OneOfTemplate")
    )

  private given Decoder[JsonOneOfTemplate] = Decoder.forProduct1("templates")(JsonOneOfTemplate.apply)

  private given Encoder[JsonNotTemplate] = t =>
    Json.obj(
      "type" -> Json.fromString("NotTemplate"),
      "template" -> t.template.asJson
    )

  private given Decoder[JsonNotTemplate] = Decoder.forProduct1("template")(JsonNotTemplate.apply)

  private given Encoder[JsonTupleTemplate] = t =>
    Json.obj(
      "itemsTemplates" -> t.itemsTemplates.asJson,
      "additionalItems" -> t.additionalItems.asJson,
      "type" -> Json.fromString("TupleTemplate")
    )

  private given Decoder[JsonTupleTemplate] =
    Decoder.forProduct2("itemsTemplates", "additionalItems")(JsonTupleTemplate.apply)

  private given Encoder[JsonNullTemplate.type] =
    _ => Json.obj("type" -> Json.fromString("NullTemplate"))

  private given Decoder[JsonNullTemplate.type] = Decoder.const(JsonNullTemplate)

  private given Encoder[JsonAnyTemplate.type] =
    _ => Json.obj("type" -> Json.fromString("AnyTemplate"))

  private given Decoder[JsonAnyTemplate.type] = Decoder.const(JsonAnyTemplate)

  private given Encoder[JsonBooleanTemplate] = t =>
    Json.obj(
      "const" -> t.const.asJson,
      "type" -> Json.fromString("BooleanTemplate")
    )

  private given Decoder[JsonBooleanTemplate] = Decoder.forProduct1("const")(JsonBooleanTemplate.apply)

  private given Encoder[JsonFloatTemplate] = t =>
    Json.obj(
      "const" -> t.const.asJson,
      "minimum" -> t.minimum.asJson,
      "maximum" -> t.maximum.asJson,
      "exclusiveMinimum" -> t.exclusiveMinimum.asJson,
      "exclusiveMaximum" -> t.exclusiveMaximum.asJson,
      "type" -> Json.fromString("FloatTemplate")
    )

  private given Decoder[JsonFloatTemplate] = Decoder.forProduct5(
    "const",
    "minimum",
    "maximum",
    "exclusiveMinimum",
    "exclusiveMaximum"
  )(
    JsonFloatTemplate.apply
  )

  private given Encoder[JsonDoubleTemplate] = t =>
    Json.obj(
      "const" -> t.const.asJson,
      "minimum" -> t.minimum.asJson,
      "maximum" -> t.maximum.asJson,
      "exclusiveMinimum" -> t.exclusiveMinimum.asJson,
      "exclusiveMaximum" -> t.exclusiveMaximum.asJson,
      "type" -> Json.fromString("DoubleTemplate")
    )

  private given Decoder[JsonDoubleTemplate] = Decoder.forProduct5(
    "const",
    "minimum",
    "maximum",
    "exclusiveMinimum",
    "exclusiveMaximum"
  )(
    JsonDoubleTemplate.apply
  )

  private given Encoder[JsonIntTemplate] = t =>
    Json.obj(
      "const" -> t.const.asJson,
      "multipleOf" -> t.multipleOf.asJson,
      "minimum" -> t.minimum.asJson,
      "maximum" -> t.maximum.asJson,
      "exclusiveMinimum" -> t.exclusiveMinimum.asJson,
      "exclusiveMaximum" -> t.exclusiveMaximum.asJson,
      "type" -> Json.fromString("IntTemplate")
    )

  private given Decoder[JsonIntTemplate] = Decoder.forProduct6(
    "const",
    "multipleOf",
    "minimum",
    "maximum",
    "exclusiveMinimum",
    "exclusiveMaximum"
  )(
    JsonIntTemplate.apply
  )

  private given Encoder[JsonLongTemplate] = t =>
    Json.obj(
      "const" -> t.const.asJson,
      "multipleOf" -> t.multipleOf.asJson,
      "minimum" -> t.minimum.asJson,
      "maximum" -> t.maximum.asJson,
      "exclusiveMinimum" -> t.exclusiveMinimum.asJson,
      "exclusiveMaximum" -> t.exclusiveMaximum.asJson,
      "type" -> Json.fromString("LongTemplate")
    )

  private given Decoder[JsonLongTemplate] = Decoder.forProduct6(
    "const",
    "multipleOf",
    "minimum",
    "maximum",
    "exclusiveMinimum",
    "exclusiveMaximum"
  )(
    JsonLongTemplate.apply
  )

  import scala.util.matching.Regex

  private given Encoder[Regex] = _.regex.asJson

  private given Decoder[Regex] = _.as[String].map(_.r)

  private given Encoder[JsonStringTemplate] = t =>
    Json.obj(
      "values" -> t.values.asJson,
      "minLength" -> t.minLength.asJson,
      "maxLength" -> t.maxLength.asJson,
      "pattern" -> t.pattern.asJson,
      "type" -> Json.fromString("StringTemplate")
    )

  private given Decoder[JsonStringTemplate] = Decoder.forProduct4(
    "values",
    "minLength",
    "maxLength",
    "pattern"
  )(JsonStringTemplate.apply)

  /** The implementation of the [[io.circe.Encoder]] type-class for the [[JsonTemplate]] type. */
  given Encoder[JsonTemplate] = {
    case t: JsonBooleanTemplate => t.asJson
    case t: JsonIntTemplate => t.asJson
    case t: JsonLongTemplate => t.asJson
    case t: JsonFloatTemplate => t.asJson
    case t: JsonDoubleTemplate => t.asJson
    case t: JsonStringTemplate => t.asJson
    case t: JsonTupleTemplate => t.asJson
    case t: JsonAnyTemplate.type => t.asJson
    case t: JsonNullTemplate.type => t.asJson
    case t: JsonAnyOfTemplate => t.asJson
    case t: JsonAllOfTemplate => t.asJson
    case t: JsonOneOfTemplate => t.asJson
    case t: JsonNotTemplate => t.asJson
  }

  /** The implementation of the [[io.circe.Decoder]] type-class for the [[JsonTemplate]] type. */
  given Decoder[JsonTemplate] = c =>
    c.get[String]("type").flatMap {
      case "NullTemplate" => c.as[JsonNullTemplate.type]
      case "BooleanTemplate" => c.as[JsonBooleanTemplate]
      case "IntTemplate" => c.as[JsonIntTemplate]
      case "LongTemplate" => c.as[JsonLongTemplate]
      case "FloatTemplate" => c.as[JsonFloatTemplate]
      case "DoubleTemplate" => c.as[JsonDoubleTemplate]
      case "StringTemplate" => c.as[JsonStringTemplate]
      case "TupleTemplate" => c.as[JsonTupleTemplate]
      case "AnyTemplate" => c.as[JsonAnyTemplate.type]
      case "AnyOfTemplate" => c.as[JsonAnyOfTemplate]
      case "AllOfTemplate" => c.as[JsonAllOfTemplate]
      case "OneOfTemplate" => c.as[JsonOneOfTemplate]
      case "NotTemplate" => c.as[JsonNotTemplate]
      case _ =>
        Left[DecodingFailure, JsonTemplate](
          DecodingFailure(
            DecodingFailure.Reason.CustomReason("The value for the type field is unacceptable"),
            c
          )
        )
    }

    /** The implementation of the [[JsonSerializable]] type-class for the [[JsonTemplate]] type. */
  given jsonTemplateJsonSerializable[T <: JsonTemplate]: JsonSerializable[T] with {

    override def serialize(tt: T): String = (tt: JsonTemplate).asJson.noSpaces
  }

  /** Extension methods for deserializing an object from a JSON-formatted string.
    *
    * The deserialization operation is the opposite to the serialization one, starting from a JSON-formatted string and leading to
    * the original object. A method should check the formatting of the string, checking that is in fact a valid JSON, and then
    * using the fields in the JSON for recreating the original object. For this reason, the deserialization method can fail and so
    * a [[scala.util.Try]] should wrap the result.
    */
  extension (s: String) {

    /** Deserializes this string into a [[scala.util.Success]] containing a [[JsonElement]], if the string is in a valid JSON
      * format and the semantic of the JSON element are followed. A [[scala.util.Failure]] containing a [[Throwable]] explaining
      * the reason of the failure in deserializing is returned otherwise.
      *
      * @return
      *   a [[JsonElement]] if the deserialization completes with success, a [[Throwable]] otherwise
      */
    def deserializeElement: Try[JsonElement] = (for {
      j <- parse(s)
      e <- j.as[JsonElement]
    } yield e).toTry

      /** Deserializes this string into a [[scala.util.Success]] containing a [[JsonTuple]], if the string is in a valid JSON
        * format and the semantic of the JSON tuple are followed. A [[scala.util.Failure]] containing a [[Throwable]] explaining
        * the reason of the failure in deserializing is returned otherwise.
        *
        * @return
        *   a [[JsonTuple]] if the deserialization completes with success, a [[Throwable]] otherwise
        */
    def deserializeTuple: Try[JsonTuple] = (for {
      j <- parse(s)
      t <- j.as[JsonTuple]
    } yield t).toTry

      /** Deserializes this string into a [[scala.util.Success]] containing a [[JsonTemplate]], if the string is in a valid JSON
        * format and the semantic of the JSON template are followed. A [[scala.util.Failure]] containing a [[Throwable]]
        * explaining the reason of the failure in deserializing is returned otherwise.
        *
        * @return
        *   a [[JsonTemplate]] if the deserialization completes with success, a [[Throwable]] otherwise
        */
    def deserializeTemplate: Try[JsonTemplate] = (for {
      j <- parse(s)
      tt <- j.as[JsonTemplate]
    } yield tt).toTry
  }
}

export JsonSerializable.*
