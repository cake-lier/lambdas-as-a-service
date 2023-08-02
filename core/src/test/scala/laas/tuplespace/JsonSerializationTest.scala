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

import org.scalatest.OptionValues.*
import org.scalatest.TryValues.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

@SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
class JsonSerializationTest extends AnyFunSpec {

  describe("A json element") {
    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        val allElements: Seq[(JsonElement, String)] =
          Seq(
            0 -> "0",
            1L -> "1",
            2.3 -> "2.3",
            4.5f -> "4.5",
            true -> "true",
            "string" -> "\"string\"",
            (null: JsonElement) -> "null",
            7 #: "LOL" #: false #: JsonNil -> "[7,\"LOL\",false]"
          )
        allElements.foreach(t => t._1.serialize shouldBe t._2)
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        val allElements: Seq[(String, JsonElement)] =
          Seq(
            "0" -> 0,
            String.valueOf(Long.MaxValue) -> Long.MaxValue,
            "2.3" -> 2.3,
            "4.5" -> 4.5f,
            "true" -> true,
            "\"string\"" -> "string",
            "null" -> (null: JsonElement),
            "[7,\"LOL\",false]" -> 7 #: "LOL" #: false #: JsonNil
          )
        allElements.foreach(t => t._1.deserializeElement.success.value shouldBe t._2)
      }
    }

    describe("when JSON deserialized with a wrongly formatted value") {
      it("should throw an exception") {
        "hello".deserializeElement.failure
      }
    }
  }

  describe("A json tuple") {
    val nestedTuple = 7 #: "LOL" #: false #: JsonNil
    val tuple = 0 #: 1L #: 2.3 #: 4.5f #: true #: "string" #: (null: JsonElement) #: nestedTuple #: JsonNil
    val repr = "[0,1,2.3,4.5,true,\"string\",null,[7,\"LOL\",false]]"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        tuple.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTuple.success.value shouldBe tuple
      }
    }

    describe("when JSON deserialized with a wrongly formatted value") {
      it("should throw an exception") {
        "hello".deserializeTuple.failure
      }
    }
  }

  describe("A json template") {
    describe("when JSON deserialized with a wrongly formatted value") {
      it("should throw an exception") {
        "hello".deserializeTemplate.failure
        "{\"const\":true,\"type\":\"TypeTemplate\"}".deserializeTemplate.failure
      }
    }
  }

  describe("A json boolean template") {
    val template = JsonBooleanTemplate(Some(true))
    val repr = "{\"const\":true,\"type\":\"BooleanTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json int template") {
    val template = JsonIntTemplate(Some(1), Some(2), Some(3), Some(4), Some(5), Some(6))
    val repr = "{\"const\":1,\"multipleOf\":2,\"minimum\":3,\"maximum\":4,\"exclusiveMinimum\":5," +
      "\"exclusiveMaximum\":6,\"type\":\"IntTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json long template") {
    val template = JsonLongTemplate(Some(1L), Some(2L), Some(3L), Some(4L), Some(5L), Some(6L))
    val repr = "{\"const\":1,\"multipleOf\":2,\"minimum\":3,\"maximum\":4,\"exclusiveMinimum\":5," +
      "\"exclusiveMaximum\":6,\"type\":\"LongTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json double template") {
    val template = JsonDoubleTemplate(Some(1.0), Some(2.0), Some(3.0), Some(4.0), Some(5.0))
    val repr = "{\"const\":1.0,\"minimum\":2.0,\"maximum\":3.0,\"exclusiveMinimum\":4.0,\"exclusiveMaximum\":5.0," +
      "\"type\":\"DoubleTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json float template") {
    val template = JsonFloatTemplate(Some(1.0f), Some(2.0f), Some(3.0f), Some(4.0f), Some(5.0f))
    val repr = "{\"const\":1.0,\"minimum\":2.0,\"maximum\":3.0,\"exclusiveMinimum\":4.0,\"exclusiveMaximum\":5.0," +
      "\"type\":\"FloatTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json string template") {
    val template = JsonStringTemplate(Some(Set("hello")), Some(1), Some(2), Some("[A-Z]+".r))
    val repr = "{\"values\":[\"hello\"],\"minLength\":1,\"maxLength\":2,\"pattern\":\"[A-Z]+\"," +
      "\"type\":\"StringTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        val deserializedTemplate = repr.deserializeTemplate.success.value
        deserializedTemplate match {
          case tt: JsonStringTemplate =>
            tt.values.value shouldBe template.values.value
            tt.minLength.value shouldBe template.minLength.value
            tt.maxLength.value shouldBe template.maxLength.value
            tt.pattern.value.pattern.pattern shouldBe template.pattern.value.pattern.pattern
          case _ => fail()
        }
      }
    }
  }

  describe("A json null template") {
    val template = JsonNullTemplate
    val repr = "{\"type\":\"NullTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json any template") {
    val template = JsonAnyTemplate
    val repr = "{\"type\":\"AnyTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json tuple template") {
    val template = JsonTupleTemplate(Seq(JsonNullTemplate, JsonAnyTemplate), additionalItems = true)
    val repr = "{\"itemsTemplates\":[{\"type\":\"NullTemplate\"},{\"type\":\"AnyTemplate\"}],\"additionalItems\":true," +
      "\"type\":\"TupleTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json not template") {
    val template = JsonNotTemplate(JsonAnyTemplate)
    val repr = "{\"type\":\"NotTemplate\",\"template\":{\"type\":\"AnyTemplate\"}}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json anyOf template") {
    val template = JsonAnyOfTemplate(Seq(JsonNullTemplate, JsonAnyTemplate))
    val repr = "{\"templates\":[{\"type\":\"NullTemplate\"},{\"type\":\"AnyTemplate\"}],\"type\":\"AnyOfTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json allOf template") {
    val template = JsonAllOfTemplate(Seq(JsonNullTemplate, JsonAnyTemplate))
    val repr = "{\"templates\":[{\"type\":\"NullTemplate\"},{\"type\":\"AnyTemplate\"}],\"type\":\"AllOfTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }

  describe("A json oneOf template") {
    val template = JsonOneOfTemplate(Seq(JsonNullTemplate, JsonAnyTemplate))
    val repr = "{\"templates\":[{\"type\":\"NullTemplate\"},{\"type\":\"AnyTemplate\"}],\"type\":\"OneOfTemplate\"}"

    describe("when JSON serialized") {
      it("should be serialized in the expected way") {
        template.serialize shouldBe repr
      }
    }

    describe("when JSON deserialized") {
      it("should be deserialized in the expected way") {
        repr.deserializeTemplate.success.value shouldBe template
      }
    }
  }
}
