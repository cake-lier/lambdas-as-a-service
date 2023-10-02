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

@SuppressWarnings(Array("scalafix:DisableSyntax.null"))
class DSLTemplateTest extends AnyFunSpec {

  describe("The complete keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json tuple template without additional items") {
        complete() shouldBe JsonTupleTemplate(Seq.empty[JsonTemplate], additionalItems = false)
      }
    }
  }

  describe("The partial keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json tuple template with additional items") {
        partial() shouldBe JsonTupleTemplate(Seq.empty[JsonTemplate], additionalItems = true)
      }
    }
  }

  describe("A tuple") {
    describe("when compiling a template via the DSL") {
      it("should create a json tuple template inside a json tuple template without additional items") {
        complete(5 #: null #: "hello" #: JsonNil) shouldBe JsonTupleTemplate(
          Seq(
            JsonTupleTemplate(
              Seq(
                JsonIntTemplate(Some(5), None, None, None, None, None),
                JsonNullTemplate,
                JsonStringTemplate(Some(Set("hello")), None, None, None)
              ),
              additionalItems = false
            )
          ),
          additionalItems = false
        )
      }
    }
  }

  describe("The anyOf keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json anyOf template") {
        complete(anyOf(*, nil)) shouldBe JsonTupleTemplate(
          Seq(JsonAnyOfTemplate(Seq(JsonAnyTemplate, JsonNullTemplate))),
          additionalItems = false
        )
      }
    }
  }

  describe("The allOf keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json allOf template") {
        complete(laas.tuplespace.allOf(*, nil)) shouldBe JsonTupleTemplate(
          Seq(JsonAllOfTemplate(Seq(JsonAnyTemplate, JsonNullTemplate))),
          additionalItems = false
        )
      }
    }
  }

  describe("The oneOf keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json oneOf template") {
        complete(laas.tuplespace.oneOf(*, nil)) shouldBe JsonTupleTemplate(
          Seq(JsonOneOfTemplate(Seq(JsonAnyTemplate, JsonNullTemplate))),
          additionalItems = false
        )
      }
    }
  }

  describe("The not keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json not template") {
        complete(laas.tuplespace.not(nil)) shouldBe JsonTupleTemplate(
          Seq(JsonNotTemplate(JsonNullTemplate)),
          additionalItems = false
        )
      }
    }
  }

  describe("The * keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json any template") {
        complete(*) shouldBe JsonTupleTemplate(Seq(JsonAnyTemplate), additionalItems = false)
      }
    }
  }

  describe("The nil keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json null template") {
        partial(nil) shouldBe JsonTupleTemplate(Seq(JsonNullTemplate), additionalItems = true)
      }
    }
  }

  describe("A boolean") {
    describe("when compiling a template via the DSL") {
      it("should create a json boolean template") {
        partial(true) shouldBe JsonTupleTemplate(
          Seq(JsonBooleanTemplate(Some(true))),
          additionalItems = true
        )
      }
    }
  }

  describe("The bool keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json boolean template") {
        partial(bool) shouldBe JsonTupleTemplate(
          Seq(JsonBooleanTemplate(None)),
          additionalItems = true
        )
      }
    }
  }

  describe("A string") {
    describe("when compiling a template via the DSL") {
      it("should create a json string template") {
        partial("hello") shouldBe JsonTupleTemplate(
          Seq(JsonStringTemplate(Some(Set("hello")), None, None, None)),
          additionalItems = true
        )
      }
    }
  }

  describe("The string keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json string template") {
        complete(string) shouldBe JsonTupleTemplate(
          Seq(JsonStringTemplate(None, None, None, None)),
          additionalItems = false
        )
        complete(string in ("hello", "test")) shouldBe JsonTupleTemplate(
          Seq(JsonStringTemplate(Some(Set("hello", "test")), None, None, None)),
          additionalItems = false
        )
        complete(string gte 3 lte 4) shouldBe JsonTupleTemplate(
          Seq(JsonStringTemplate(None, Some(3), Some(4), None)),
          additionalItems = false
        )
        complete(string gte 3) shouldBe JsonTupleTemplate(
          Seq(JsonStringTemplate(None, Some(3), None, None)),
          additionalItems = false
        )
        complete(string lte 4) shouldBe JsonTupleTemplate(
          Seq(JsonStringTemplate(None, None, Some(4), None)),
          additionalItems = false
        )
        complete(string lte 4 gte 3) shouldBe JsonTupleTemplate(
          Seq(JsonStringTemplate(None, Some(3), Some(4), None)),
          additionalItems = false
        )
        val regex = "[A-Z]+".r
        complete(string matches regex) shouldBe JsonTupleTemplate(
          Seq(JsonStringTemplate(None, None, None, Some(regex))),
          additionalItems = false
        )
      }
    }
  }

  describe("A float") {
    describe("when compiling a template via the DSL") {
      it("should create a json float template with a constant element") {
        complete(4.5f) shouldBe
        JsonTupleTemplate(Seq(JsonFloatTemplate(Some(4.5f), None, None, None, None)), additionalItems = false)
      }
    }
  }

  describe("The float keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json float template") {
        complete(float) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              None,
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(float lt 1.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              None,
              None,
              None,
              Some(1.0f)
            )
          ),
          additionalItems = false
        )
        complete(float gte 4.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              Some(4.0f),
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(float gt 3.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              None,
              None,
              Some(3.0f),
              None
            )
          ),
          additionalItems = false
        )
        complete(float lte 2.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              None,
              Some(2.0f),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(float lt 1.0f gte 4.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              Some(4.0f),
              None,
              None,
              Some(1.0f)
            )
          ),
          additionalItems = false
        )
        complete(float gte 4.0f lt 1.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              Some(4.0f),
              None,
              None,
              Some(1.0f)
            )
          ),
          additionalItems = false
        )
        complete(float lte 2.0f gt 3.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              None,
              Some(2.0f),
              Some(3.0f),
              None
            )
          ),
          additionalItems = false
        )
        complete(float gt 3.0f lte 2.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              None,
              Some(2.0f),
              Some(3.0f),
              None
            )
          ),
          additionalItems = false
        )
        complete(float lt 1.0f gt 4.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              None,
              None,
              Some(4.0f),
              Some(1.0f)
            )
          ),
          additionalItems = false
        )
        complete(float gt 4.0f lt 1.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              None,
              None,
              Some(4.0f),
              Some(1.0f)
            )
          ),
          additionalItems = false
        )
        complete(float lte 2.0f gte 3.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              Some(3.0f),
              Some(2.0f),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(float gte 3.0f lte 2.0f) shouldBe JsonTupleTemplate(
          Seq(
            JsonFloatTemplate(
              None,
              Some(3.0f),
              Some(2.0f),
              None,
              None
            )
          ),
          additionalItems = false
        )
      }
    }
  }

  describe("A double") {
    describe("when compiling a template via the DSL") {
      it("should create a json double template with a constant element") {
        complete(0.99) shouldBe
        JsonTupleTemplate(Seq(JsonDoubleTemplate(Some(0.99), None, None, None, None)), additionalItems = false)
      }
    }
  }

  describe("The double keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json double template") {
        complete(double) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              None,
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(double lt 0.99) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              None,
              None,
              None,
              Some(0.99)
            )
          ),
          additionalItems = false
        )
        complete(double gte 1.5e11) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              Some(1.5e11),
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(double gt -7.5e13) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              None,
              None,
              Some(-7.5e13),
              None
            )
          ),
          additionalItems = false
        )
        complete(double lte -7.5e-2) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              None,
              Some(-7.5e-2),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(double lt 0.99 gte 1.5e11) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              Some(1.5e11),
              None,
              None,
              Some(0.99)
            )
          ),
          additionalItems = false
        )
        complete(double gte 1.5e11 lt 0.99) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              Some(1.5e11),
              None,
              None,
              Some(0.99)
            )
          ),
          additionalItems = false
        )
        complete(double lte -7.5e-2 gt -7.5e13) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              None,
              Some(-7.5e-2),
              Some(-7.5e13),
              None
            )
          ),
          additionalItems = false
        )
        complete(double gt -7.5e13 lte -7.5e-2) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              None,
              Some(-7.5e-2),
              Some(-7.5e13),
              None
            )
          ),
          additionalItems = false
        )
        complete(double lt 0.99 gt 1.5e11) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              None,
              None,
              Some(1.5e11),
              Some(0.99)
            )
          ),
          additionalItems = false
        )
        complete(double gt 1.5e11 lt 0.99) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              None,
              None,
              Some(1.5e11),
              Some(0.99)
            )
          ),
          additionalItems = false
        )
        complete(double lte -7.5e-2 gte -7.5e13) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              Some(-7.5e13),
              Some(-7.5e-2),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(double gte -7.5e13 lte -7.5e-2) shouldBe JsonTupleTemplate(
          Seq(
            JsonDoubleTemplate(
              None,
              Some(-7.5e13),
              Some(-7.5e-2),
              None,
              None
            )
          ),
          additionalItems = false
        )
      }
    }
  }
}
