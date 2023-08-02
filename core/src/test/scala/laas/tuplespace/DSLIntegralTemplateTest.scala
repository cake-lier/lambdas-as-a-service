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

class DSLIntegralTemplateTest extends AnyFunSpec {

  describe("An int") {
    describe("when compiling a template via the DSL") {
      it("should create a json int template with a constant element") {
        complete(42) shouldBe
        JsonTupleTemplate(Seq(JsonIntTemplate(Some(42), None, None, None, None, None)), additionalItems = false)
      }
    }
  }

  describe("The int keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json int template") {
        complete(int) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              None,
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int lt -3) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              None,
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int gte -2) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              Some(-2),
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int gt 51) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              None,
              None,
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int lte 24) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              None,
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int lt -3 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int div 9 lt -3) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int div 9 gte -2) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(-2),
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int gte -2 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(-2),
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int div 9 gt 51) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int gt 51 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int div 9 lte 24) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int lte 24 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int lt -3 gte -2) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              Some(-2),
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int gte -2 lt -3) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              Some(-2),
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int lte 24 gt 51) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              None,
              Some(24),
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int gt 51 lte 24) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              None,
              Some(24),
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int lt -3 gt -2) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              None,
              None,
              Some(-2),
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int gt -2 lt -3) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              None,
              None,
              Some(-2),
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int lte 24 gte 51) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              Some(51),
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int gte 51 lte 24) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              None,
              Some(51),
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int lt -3 gte -2 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(-2),
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int gte -2 lt -3 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(-2),
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int lte 24 gt 51 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              Some(24),
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int gt 51 lte 24 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              Some(24),
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int lt -3 gt -2 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              Some(-2),
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int gt -2 lt -3 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              Some(-2),
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int lte 24 gte 51 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(51),
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int gte 51 lte 24 div 9) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(51),
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int lt -3 div 9 gte -2) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(-2),
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int gte -2 div 9 lt -3) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(-2),
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int lte 24 div 9 gt 51) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              Some(24),
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int gt 51 div 9 lte 24) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              Some(24),
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int lt -3 div 9 gt -2) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              Some(-2),
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int gt -2 div 9 lt -3) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              Some(-2),
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int lte 24 div 9 gte 51) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(51),
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int gte 51 div 9 lte 24) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(51),
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int div 9 lt -3 gte -2) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(-2),
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int div 9 gte -2 lt -3) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(-2),
              None,
              None,
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int div 9 lte 24 gt 51) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              Some(24),
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int div 9 gt 51 lte 24) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              Some(24),
              Some(51),
              None
            )
          ),
          additionalItems = false
        )
        complete(int div 9 lt -3 gt -2) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              Some(-2),
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int div 9 gt -2 lt -3) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              None,
              None,
              Some(-2),
              Some(-3)
            )
          ),
          additionalItems = false
        )
        complete(int div 9 lte 24 gte 51) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(51),
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(int div 9 gte 51 lte 24) shouldBe JsonTupleTemplate(
          Seq(
            JsonIntTemplate(
              None,
              Some(9),
              Some(51),
              Some(24),
              None,
              None
            )
          ),
          additionalItems = false
        )
      }
    }
  }

  describe("A long") {
    describe("when compiling a template via the DSL") {
      it("should create a json long template with a constant element") {
        complete(42L) shouldBe
        JsonTupleTemplate(Seq(JsonLongTemplate(Some(42L), None, None, None, None, None)), additionalItems = false)
      }
    }
  }

  describe("The long keyword") {
    describe("when compiling a template via the DSL") {
      it("should create a json long template") {
        complete(long) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              None,
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long lt -3L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              None,
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long gte -2L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              Some(-2L),
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long gt 51L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              None,
              None,
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long lte 24L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              None,
              Some(24L),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long lt -3L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long div 9L lt -3L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long div 9L gte -2L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(-2L),
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long gte -2L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(-2L),
              None,
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long div 9L gt 51L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long gt 51L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long div 9L lte 24L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              Some(24L),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long lte 24L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              Some(24L),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long lt -3L gte -2L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              Some(-2L),
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long gte -2L lt -3L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              Some(-2L),
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long lte 24L gt 51L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              None,
              Some(24L),
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long gt 51L lte 24L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              None,
              Some(24L),
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long lt -3L gt -2L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              None,
              None,
              Some(-2L),
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long gt -2L lt -3L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              None,
              None,
              Some(-2L),
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long lte 24L gte 51L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              Some(51L),
              Some(24L),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long gte 51L lte 24L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              None,
              Some(51L),
              Some(24L),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long lt -3L gte -2L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(-2L),
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long gte -2L lt -3L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(-2L),
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long lte 24L gt 51L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              Some(24L),
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long gt 51L lte 24L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              Some(24L),
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long lt -3L gt -2L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              Some(-2L),
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long gt -2L lt -3L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              Some(-2L),
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long lte 24L gte 51L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(51L),
              Some(24L),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long gte 51L lte 24L div 9L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(51L),
              Some(24L),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long lt -3L div 9L gte -2L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(-2L),
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long gte -2L div 9L lt -3L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(-2L),
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long lte 24L div 9L gt 51L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              Some(24L),
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long gt 51L div 9L lte 24L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              Some(24L),
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long lt -3L div 9L gt -2L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              Some(-2L),
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long gt -2L div 9L lt -3L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              Some(-2L),
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long lte 24L div 9L gte 51L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(51L),
              Some(24L),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long gte 51L div 9L lte 24L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(51L),
              Some(24L),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long div 9L lt -3L gte -2L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(-2L),
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long div 9L gte -2L lt -3L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(-2L),
              None,
              None,
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long div 9L lte 24L gt 51L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              Some(24L),
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long div 9L gt 51L lte 24L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              Some(24L),
              Some(51L),
              None
            )
          ),
          additionalItems = false
        )
        complete(long div 9L lt -3L gt -2L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              Some(-2L),
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long div 9L gt -2L lt -3L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              None,
              None,
              Some(-2L),
              Some(-3L)
            )
          ),
          additionalItems = false
        )
        complete(long div 9L lte 24L gte 51L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(51L),
              Some(24L),
              None,
              None
            )
          ),
          additionalItems = false
        )
        complete(long div 9L gte 51L lte 24L) shouldBe JsonTupleTemplate(
          Seq(
            JsonLongTemplate(
              None,
              Some(9L),
              Some(51L),
              Some(24L),
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
