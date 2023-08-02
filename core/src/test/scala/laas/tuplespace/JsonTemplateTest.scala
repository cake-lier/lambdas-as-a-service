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

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

@SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
class JsonTemplateTest extends AnyFunSpec {
  private val booleanValue = true
  private val intValue = 5
  private val longValue = 7L
  private val doubleValue = 2.3
  private val floatValue = 4.5f
  private val stringValue = "hello"
  private val nullValue: Null = null

  private val jsonTupleValue = JsonTuple(
    booleanValue,
    intValue,
    longValue,
    doubleValue,
    floatValue,
    stringValue,
    nullValue,
    JsonTuple("test", 300L)
  )

  describe("A json boolean template") {
    describe("when used to match a json boolean value") {
      it("should correctly match it") {
        JsonBooleanTemplate(None).matches(booleanValue) shouldBe true
        JsonBooleanTemplate(Some(true)).matches(booleanValue) shouldBe true
        JsonBooleanTemplate(Some(false)).matches(booleanValue) shouldBe false
      }
    }

    describe("when used to match a different json value") {
      it("should not match it under any circumstance") {
        JsonBooleanTemplate(None).matches(stringValue) shouldBe false
        JsonBooleanTemplate(Some(true)).matches(intValue) shouldBe false
        JsonBooleanTemplate(Some(false)).matches(doubleValue) shouldBe false
      }
    }
  }

  describe("A json int template") {
    describe("when used to match a json int value") {
      it("should correctly match it") {
        JsonIntTemplate(None, None, None, None, None, None).matches(intValue) shouldBe true
        JsonIntTemplate(Some(5), None, None, None, None, None).matches(intValue) shouldBe true
        JsonIntTemplate(Some(42), None, None, None, None, None).matches(intValue) shouldBe false
        JsonIntTemplate(None, Some(1), None, None, None, None).matches(intValue) shouldBe true
        JsonIntTemplate(None, Some(2), None, None, None, None).matches(intValue) shouldBe false
        JsonIntTemplate(None, None, Some(5), None, None, None).matches(intValue) shouldBe true
        JsonIntTemplate(None, None, Some(6), None, None, None).matches(intValue) shouldBe false
        JsonIntTemplate(None, None, None, Some(5), None, None).matches(intValue) shouldBe true
        JsonIntTemplate(None, None, None, Some(4), None, None).matches(intValue) shouldBe false
        JsonIntTemplate(None, None, None, None, Some(4), None).matches(intValue) shouldBe true
        JsonIntTemplate(None, None, None, None, Some(5), None).matches(intValue) shouldBe false
        JsonIntTemplate(None, None, None, None, None, Some(6)).matches(intValue) shouldBe true
        JsonIntTemplate(None, None, None, None, None, Some(5)).matches(intValue) shouldBe false
      }
    }

    describe("when used to match a different json value") {
      it("should not match it under any circumstance") {
        JsonIntTemplate(None, None, None, None, None, None).matches(longValue) shouldBe false
        JsonIntTemplate(Some(5), None, None, None, None, None).matches(floatValue) shouldBe false
        JsonIntTemplate(Some(42), None, None, None, None, None).matches(doubleValue) shouldBe false
        JsonIntTemplate(None, Some(1), None, None, None, None).matches(stringValue) shouldBe false
        JsonIntTemplate(None, Some(2), None, None, None, None).matches(nullValue) shouldBe false
        JsonIntTemplate(None, None, Some(5), None, None, None).matches(booleanValue) shouldBe false
        JsonIntTemplate(None, None, Some(6), None, None, None).matches(longValue) shouldBe false
        JsonIntTemplate(None, None, None, Some(6), None, None).matches(floatValue) shouldBe false
        JsonIntTemplate(None, None, None, Some(5), None, None).matches(doubleValue) shouldBe false
        JsonIntTemplate(None, None, None, None, Some(4), None).matches(stringValue) shouldBe false
        JsonIntTemplate(None, None, None, None, Some(5), None).matches(nullValue) shouldBe false
        JsonIntTemplate(None, None, None, None, None, Some(6)).matches(booleanValue) shouldBe false
        JsonIntTemplate(None, None, None, None, None, Some(5)).matches(longValue) shouldBe false
      }
    }
  }

  describe("A json long template") {
    describe("when used to match a json long value") {
      it("should correctly match it") {
        JsonLongTemplate(None, None, None, None, None, None).matches(longValue) shouldBe true
        JsonLongTemplate(Some(7L), None, None, None, None, None).matches(longValue) shouldBe true
        JsonLongTemplate(Some(42L), None, None, None, None, None).matches(longValue) shouldBe false
        JsonLongTemplate(None, Some(1L), None, None, None, None).matches(longValue) shouldBe true
        JsonLongTemplate(None, Some(2L), None, None, None, None).matches(longValue) shouldBe false
        JsonLongTemplate(None, None, Some(7), None, None, None).matches(longValue) shouldBe true
        JsonLongTemplate(None, None, Some(8), None, None, None).matches(longValue) shouldBe false
        JsonLongTemplate(None, None, None, Some(7), None, None).matches(longValue) shouldBe true
        JsonLongTemplate(None, None, None, Some(6), None, None).matches(longValue) shouldBe false
        JsonLongTemplate(None, None, None, None, Some(6), None).matches(longValue) shouldBe true
        JsonLongTemplate(None, None, None, None, Some(7), None).matches(longValue) shouldBe false
        JsonLongTemplate(None, None, None, None, None, Some(8)).matches(longValue) shouldBe true
        JsonLongTemplate(None, None, None, None, None, Some(7)).matches(longValue) shouldBe false
      }
    }

    describe("when used to match a different json value") {
      it("should not match it under any circumstance") {
        JsonLongTemplate(None, None, None, None, None, None).matches(intValue) shouldBe false
        JsonLongTemplate(Some(5), None, None, None, None, None).matches(floatValue) shouldBe false
        JsonLongTemplate(Some(42), None, None, None, None, None).matches(doubleValue) shouldBe false
        JsonLongTemplate(None, Some(1), None, None, None, None).matches(stringValue) shouldBe false
        JsonLongTemplate(None, Some(2), None, None, None, None).matches(nullValue) shouldBe false
        JsonLongTemplate(None, None, Some(-4), None, None, None).matches(booleanValue) shouldBe false
        JsonLongTemplate(None, None, Some(10), None, None, None).matches(intValue) shouldBe false
        JsonLongTemplate(None, None, None, Some(10), None, None).matches(floatValue) shouldBe false
        JsonLongTemplate(None, None, None, Some(-4), None, None).matches(doubleValue) shouldBe false
        JsonLongTemplate(None, None, None, None, Some(4), None).matches(stringValue) shouldBe false
        JsonLongTemplate(None, None, None, None, Some(5), None).matches(nullValue) shouldBe false
        JsonLongTemplate(None, None, None, None, None, Some(6)).matches(booleanValue) shouldBe false
        JsonLongTemplate(None, None, None, None, None, Some(5)).matches(intValue) shouldBe false
      }
    }
  }

  describe("A json double template") {
    describe("when used to match a json double value") {
      it("should correctly match it") {
        JsonDoubleTemplate(None, None, None, None, None).matches(doubleValue) shouldBe true
        JsonDoubleTemplate(Some(2.3), None, None, None, None).matches(doubleValue) shouldBe true
        JsonDoubleTemplate(Some(7e-5), None, None, None, None).matches(doubleValue) shouldBe false
        JsonDoubleTemplate(None, Some(2.3), None, None, None).matches(doubleValue) shouldBe true
        JsonDoubleTemplate(None, Some(2.4), None, None, None).matches(doubleValue) shouldBe false
        JsonDoubleTemplate(None, None, Some(2.3), None, None).matches(doubleValue) shouldBe true
        JsonDoubleTemplate(None, None, Some(0.1), None, None).matches(doubleValue) shouldBe false
        JsonDoubleTemplate(None, None, None, Some(0.99), None).matches(doubleValue) shouldBe true
        JsonDoubleTemplate(None, None, None, Some(2.3), None).matches(doubleValue) shouldBe false
        JsonDoubleTemplate(None, None, None, None, Some(10e9)).matches(doubleValue) shouldBe true
        JsonDoubleTemplate(None, None, None, None, Some(2.3)).matches(doubleValue) shouldBe false
      }
    }

    describe("when used to match a different json value") {
      it("should not match it under any circumstance") {
        JsonDoubleTemplate(None, None, None, None, None).matches(intValue) shouldBe false
        JsonDoubleTemplate(Some(5), None, None, None, None).matches(floatValue) shouldBe false
        JsonDoubleTemplate(Some(42), None, None, None, None).matches(longValue) shouldBe false
        JsonDoubleTemplate(None, None, Some(-4), None, None).matches(booleanValue) shouldBe false
        JsonDoubleTemplate(None, None, Some(10), None, None).matches(intValue) shouldBe false
        JsonDoubleTemplate(None, None, None, Some(10), None).matches(floatValue) shouldBe false
        JsonDoubleTemplate(None, None, None, Some(-4), None).matches(longValue) shouldBe false
        JsonDoubleTemplate(None, None, None, None, Some(4)).matches(stringValue) shouldBe false
        JsonDoubleTemplate(None, None, None, None, Some(5)).matches(nullValue) shouldBe false
        JsonDoubleTemplate(None, None, None, None, Some(6)).matches(booleanValue) shouldBe false
        JsonDoubleTemplate(None, None, None, None, Some(5)).matches(intValue) shouldBe false
      }
    }
  }

  describe("A json float template") {
    describe("when used to match a json float value") {
      it("should correctly match it") {
        JsonFloatTemplate(None, None, None, None, None).matches(floatValue) shouldBe true
        JsonFloatTemplate(Some(4.5f), None, None, None, None).matches(floatValue) shouldBe true
        JsonFloatTemplate(Some(7e-5f), None, None, None, None).matches(floatValue) shouldBe false
        JsonFloatTemplate(None, Some(4.5f), None, None, None).matches(floatValue) shouldBe true
        JsonFloatTemplate(None, Some(100.1f), None, None, None).matches(floatValue) shouldBe false
        JsonFloatTemplate(None, None, Some(4.5f), None, None).matches(floatValue) shouldBe true
        JsonFloatTemplate(None, None, Some(0.1f), None, None).matches(floatValue) shouldBe false
        JsonFloatTemplate(None, None, None, Some(0.99f), None).matches(floatValue) shouldBe true
        JsonFloatTemplate(None, None, None, Some(4.5f), None).matches(floatValue) shouldBe false
        JsonFloatTemplate(None, None, None, None, Some(10e9f)).matches(floatValue) shouldBe true
        JsonFloatTemplate(None, None, None, None, Some(4.5f)).matches(floatValue) shouldBe false
      }
    }

    describe("when used to match a different json value") {
      it("should not match it under any circumstance") {
        JsonFloatTemplate(None, None, None, None, None).matches(intValue) shouldBe false
        JsonFloatTemplate(Some(5), None, None, None, None).matches(doubleValue) shouldBe false
        JsonFloatTemplate(Some(42), None, None, None, None).matches(longValue) shouldBe false
        JsonFloatTemplate(None, None, Some(-4), None, None).matches(booleanValue) shouldBe false
        JsonFloatTemplate(None, None, Some(10), None, None).matches(intValue) shouldBe false
        JsonFloatTemplate(None, None, None, Some(10), None).matches(doubleValue) shouldBe false
        JsonFloatTemplate(None, None, None, Some(-4), None).matches(longValue) shouldBe false
        JsonFloatTemplate(None, None, None, None, Some(4)).matches(stringValue) shouldBe false
        JsonFloatTemplate(None, None, None, None, Some(5)).matches(nullValue) shouldBe false
        JsonFloatTemplate(None, None, None, None, Some(6)).matches(booleanValue) shouldBe false
        JsonFloatTemplate(None, None, None, None, Some(5)).matches(intValue) shouldBe false
      }
    }
  }

  describe("A json string template") {
    describe("when used to match a json string value") {
      it("should correctly match it") {
        JsonStringTemplate(None, None, None, None).matches(stringValue) shouldBe true
        JsonStringTemplate(Some(Set("hello")), None, None, None).matches(stringValue) shouldBe true
        JsonStringTemplate(Some(Set("hello", "lol")), None, None, None).matches(stringValue) shouldBe true
        JsonStringTemplate(Some(Set.empty[String]), None, None, None).matches(stringValue) shouldBe false
        JsonStringTemplate(Some(Set("none")), None, None, None).matches(stringValue) shouldBe false
        JsonStringTemplate(None, Some(5), None, None).matches(stringValue) shouldBe true
        JsonStringTemplate(None, Some(6), None, None).matches(stringValue) shouldBe false
        JsonStringTemplate(None, None, Some(5), None).matches(stringValue) shouldBe true
        JsonStringTemplate(None, None, Some(4), None).matches(stringValue) shouldBe false
        JsonStringTemplate(None, None, None, Some("[A-Za-z]+".r)).matches(stringValue) shouldBe true
        JsonStringTemplate(None, None, None, Some("[0-9]+".r)).matches(stringValue) shouldBe false
      }
    }

    describe("when used to match a different json value") {
      it("should not match it under any circumstance") {
        JsonStringTemplate(None, None, None, None).matches(intValue) shouldBe false
        JsonStringTemplate(Some(Set("hello")), None, None, None).matches(longValue) shouldBe false
        JsonStringTemplate(Some(Set("hello", "lol")), None, None, None).matches(floatValue) shouldBe false
        JsonStringTemplate(Some(Set.empty[String]), None, None, None).matches(doubleValue) shouldBe false
        JsonStringTemplate(Some(Set("none")), None, None, None).matches(booleanValue) shouldBe false
        JsonStringTemplate(None, Some(5), None, None).matches(nullValue) shouldBe false
        JsonStringTemplate(None, Some(6), None, None).matches(jsonTupleValue) shouldBe false
        JsonStringTemplate(None, None, Some(5), None).matches(intValue) shouldBe false
        JsonStringTemplate(None, None, Some(4), None).matches(nullValue) shouldBe false
        JsonStringTemplate(None, None, None, Some("[A-Za-z]+".r)).matches(jsonTupleValue) shouldBe false
        JsonStringTemplate(None, None, None, Some("[0-9]+".r)).matches(doubleValue) shouldBe false
      }
    }

    describe("when used in an equals comparison") {
      it("should return the correct value for equals and hashCode") {
        val template = JsonStringTemplate(None, None, None, Some("[A-Za-z]+".r))
        val equalTemplate = JsonStringTemplate(None, None, None, Some("[A-Za-z]+".r))
        val anotherEqualTemplate = JsonStringTemplate(None, None, None, Some("[A-Za-z]+".r))
        val nonEqualTemplate = JsonStringTemplate(None, None, None, Some("[0-9]+".r))

        template shouldEqual equalTemplate
        equalTemplate shouldEqual template
        template shouldEqual template
        equalTemplate shouldEqual anotherEqualTemplate
        template shouldEqual anotherEqualTemplate

        template should not equal nonEqualTemplate

        template.hashCode shouldEqual equalTemplate.hashCode
      }
    }
  }

  describe("A json null template") {
    describe("when used to match a json null value") {
      it("should correctly match it") {
        JsonNullTemplate.matches(null) shouldBe true
      }
    }

    describe("when used to match a different json value") {
      it("should not match it under any circumstance") {
        JsonNullTemplate.matches(intValue) shouldBe false
        JsonNullTemplate.matches(longValue) shouldBe false
        JsonNullTemplate.matches(floatValue) shouldBe false
        JsonNullTemplate.matches(doubleValue) shouldBe false
        JsonNullTemplate.matches(booleanValue) shouldBe false
        JsonNullTemplate.matches(stringValue) shouldBe false
        JsonNullTemplate.matches(jsonTupleValue) shouldBe false
      }
    }
  }

  describe("A json any template") {
    describe("when used to match any json value") {
      it("should correctly match it") {
        JsonAnyTemplate.matches(intValue) shouldBe true
        JsonAnyTemplate.matches(longValue) shouldBe true
        JsonAnyTemplate.matches(floatValue) shouldBe true
        JsonAnyTemplate.matches(doubleValue) shouldBe true
        JsonAnyTemplate.matches(booleanValue) shouldBe true
        JsonAnyTemplate.matches(stringValue) shouldBe true
        JsonAnyTemplate.matches(jsonTupleValue) shouldBe true
      }
    }
  }

  describe("A json tuple template") {
    describe("when used to match exactly a json tuple value") {
      it("should correctly match it") {
        JsonTupleTemplate(
          Seq(
            JsonBooleanTemplate(None),
            JsonIntTemplate(None, Some(5), None, None, None, None),
            JsonLongTemplate(None, None, Some(1), None, None, None),
            JsonDoubleTemplate(None, None, None, Some(0.99), None),
            JsonFloatTemplate(None, None, None, None, Some(10e9f)),
            JsonStringTemplate(Some(Set("hello", "lol")), None, None, None),
            JsonNullTemplate,
            JsonTupleTemplate(Seq(JsonAnyTemplate, JsonAnyTemplate), additionalItems = false)
          ),
          additionalItems = false
        ).matches(jsonTupleValue) shouldBe true
      }
    }

    describe("when used to match partially a json tuple value") {
      it("should correctly match it starting from the leftmost element of it") {
        JsonTupleTemplate(
          Seq(
            JsonBooleanTemplate(None),
            JsonIntTemplate(None, Some(5), None, None, None, None),
            JsonLongTemplate(None, None, Some(1), None, None, None),
            JsonDoubleTemplate(None, None, None, Some(0.99), None),
            JsonFloatTemplate(None, None, None, None, Some(10e9f))
          ),
          additionalItems = true
        ).matches(jsonTupleValue) shouldBe true
      }
    }

    describe("when used to match partially a different json value") {
      it("should not match it under any circumstance") {
        JsonTupleTemplate(Seq(JsonBooleanTemplate(None)), additionalItems = true).matches(booleanValue) shouldBe false
        JsonTupleTemplate(Seq(JsonIntTemplate(None, None, None, None, None, None)), additionalItems = true)
          .matches(intValue) shouldBe false
        JsonTupleTemplate(Seq(JsonLongTemplate(None, None, None, None, None, None)), additionalItems = true)
          .matches(longValue) shouldBe false
        JsonTupleTemplate(Seq(JsonFloatTemplate(None, None, None, None, None)), additionalItems = true)
          .matches(floatValue) shouldBe false
        JsonTupleTemplate(Seq(JsonDoubleTemplate(None, None, None, None, None)), additionalItems = true)
          .matches(doubleValue) shouldBe false
        JsonTupleTemplate(Seq(JsonStringTemplate(None, None, None, None)), additionalItems = true)
          .matches(stringValue) shouldBe false
        JsonTupleTemplate(Seq(JsonNullTemplate), additionalItems = true).matches(nullValue) shouldBe false
        JsonTupleTemplate(Seq(JsonTupleTemplate(Seq.empty, false)), additionalItems = true).matches(jsonTupleValue) shouldBe false
      }
    }
  }

  describe("A json not template") {
    describe("when used on another template") {
      it("should return the opposite answer that the first template returns") {
        JsonNotTemplate(JsonBooleanTemplate(Some(true))).matches(booleanValue) shouldBe false
        JsonNotTemplate(JsonBooleanTemplate(Some(false))).matches(booleanValue) shouldBe true
      }
    }
  }

  describe("A json anyOf template") {
    describe("when used on other templates") {
      it("should match only if at least one of those templates match") {
        JsonAnyOfTemplate(Seq(JsonBooleanTemplate(Some(true)), JsonNullTemplate)).matches(booleanValue) shouldBe true
        JsonAnyOfTemplate(Seq(JsonBooleanTemplate(Some(false)), JsonNullTemplate)).matches(booleanValue) shouldBe false
        JsonAnyOfTemplate(Seq(JsonBooleanTemplate(None), JsonBooleanTemplate(Some(true)))).matches(booleanValue) shouldBe true
      }
    }
  }

  describe("A json allOf template") {
    describe("when used on other templates") {
      it("should match only if all those templates match") {
        JsonAllOfTemplate(Seq(JsonBooleanTemplate(Some(true)), JsonNullTemplate)).matches(booleanValue) shouldBe false
        JsonAllOfTemplate(Seq(JsonBooleanTemplate(Some(false)), JsonNullTemplate)).matches(booleanValue) shouldBe false
        JsonAllOfTemplate(Seq(JsonBooleanTemplate(None), JsonBooleanTemplate(Some(true)))).matches(booleanValue) shouldBe true
      }
    }
  }

  describe("A json oneOf template") {
    describe("when used on other templates") {
      it("should match only if exactly one of those templates match") {
        JsonOneOfTemplate(Seq(JsonBooleanTemplate(Some(true)), JsonNullTemplate)).matches(booleanValue) shouldBe true
        JsonOneOfTemplate(Seq(JsonBooleanTemplate(Some(false)), JsonNullTemplate)).matches(booleanValue) shouldBe false
        JsonOneOfTemplate(Seq(JsonBooleanTemplate(None), JsonBooleanTemplate(Some(true)))).matches(booleanValue) shouldBe false
      }
    }
  }
}
