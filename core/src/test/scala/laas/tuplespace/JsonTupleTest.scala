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
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import AnyOps.*

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Null",
    "org.wartremover.warts.ToString",
    "org.wartremover.warts.Var",
    "scalafix:DisableSyntax.null",
    "scalafix:DisableSyntax.var"
  )
)
class JsonTupleTest extends AnyFunSpec {
  private val tuple0 = JsonTuple()
  private val tuple1 = JsonTuple(0)
  private val tuple3 = JsonTuple("hello", true, JsonTuple(2.3, null))

  describe("A json tuple") {
    describe("when created empty") {
      it("should be empty") {
        tuple0 shouldBe JsonNil
        tuple0.toString shouldBe "()"
      }
    }

    describe("when created with only one element") {
      it("should contain that element") {
        tuple1 shouldBe 0 #: JsonNil
        tuple1.toString shouldBe "(0)"
      }
    }

    describe("when created with multiple elements") {
      it("should contain them all") {
        tuple3 shouldBe "hello" #: true #: (2.3 #: null #: JsonNil) #: JsonNil
        tuple3.toString shouldBe "(hello, true, (2.3, null))"
      }
    }

    describe("when created from a Seq") {
      it("should contain all Seq elements") {
        val seq = Seq[JsonElement](0, "hello", true, null)
        JsonTuple.fromSeq(seq) shouldBe 0 #: "hello" #: true #: null #: JsonNil
        JsonTuple.fromSeq(Seq.empty[JsonElement]) shouldBe JsonNil
      }
    }

    describe("when calculating its arity") {
      it("should return the correct value") {
        tuple0.arity shouldBe 0
        tuple1.arity shouldBe 1
        tuple3.arity shouldBe 3
      }
    }

    describe("when appending an element") {
      it("should have the element appended") {
        val value = 2.3 #: 1L #: JsonNil

        tuple0 :# value shouldBe value #: JsonNil
        tuple1 :# value shouldBe 0 #: value #: JsonNil
        tuple3 :# value shouldBe "hello" #: true #: (2.3 #: null #: JsonNil) #: value #: JsonNil
        JsonNil :# tuple0 shouldBe JsonNil #: JsonNil
        tuple3 :# JsonNil shouldBe "hello" #: true #: (2.3 #: null #: JsonNil) #: JsonNil #: JsonNil
      }
    }

    describe("when concatenating another tuple") {
      it("should create a new tuple with the elements of the second appended to the first") {
        val value = 2.3 #: 1L #: JsonNil

        tuple0 :## value shouldBe value
        tuple1 :## value shouldBe 0 #: 2.3 #: 1L #: JsonNil
        tuple3 :## value shouldBe "hello" #: true #: (2.3 #: null #: JsonNil) #: 2.3 #: 1L #: JsonNil
        JsonNil :## tuple0 shouldBe JsonNil
        tuple3 :## JsonNil shouldBe tuple3
      }
    }

    describe("when dropping some elements") {
      it("should drop the specified elements") {
        tuple0.drop(-1) shouldBe JsonNil
        tuple0.drop(0) shouldBe JsonNil
        tuple0.drop(1) shouldBe JsonNil
        tuple0.drop(5) shouldBe JsonNil
        tuple1.drop(-1) shouldBe tuple1
        tuple1.drop(0) shouldBe tuple1
        tuple1.drop(1) shouldBe JsonNil
        tuple1.drop(5) shouldBe JsonNil
        tuple3.drop(-1) shouldBe tuple3
        tuple3.drop(0) shouldBe tuple3
        tuple3.drop(1) shouldBe true #: (2.3 #: null #: JsonNil) #: JsonNil
        tuple3.drop(5) shouldBe JsonNil
      }
    }

    describe("when extracting some elements") {
      it("should given the specified element if it exists") {
        tuple0.elem(-1) shouldBe empty
        tuple0.elem(0) shouldBe empty
        tuple0.elem(1) shouldBe empty
        tuple0.elem(5) shouldBe empty
        tuple1.elem(-1) shouldBe empty
        tuple1.elem(0).value shouldBe 0
        tuple1.elem(1) shouldBe empty
        tuple1.elem(5) shouldBe empty
        tuple3.elem(-1) shouldBe empty
        tuple3.elem(0).value shouldBe "hello"
        tuple3.elem(1).value shouldBe true
        tuple3.elem(5) shouldBe empty
      }
    }

    describe("when filtering some elements") {
      it("should keep the elements passing the filter") {
        tuple0.filter(_ => false) shouldBe JsonNil
        tuple0.filter(_ => true) shouldBe JsonNil
        tuple0.filter(_ !== (2.3 #: null #: JsonNil)) shouldBe JsonNil
        tuple1.filter(_ => false) shouldBe JsonNil
        tuple1.filter(_ => true) shouldBe tuple1
        tuple1.filter(_ !== 0) shouldBe JsonNil
        tuple3.filter(_ => false) shouldBe JsonNil
        tuple3.filter(_ => true) shouldBe tuple3
        tuple3.filter(_ !== true) shouldBe "hello" #: (2.3 #: null #: JsonNil) #: JsonNil
        tuple3.filter(_ !== "hello") shouldBe true #: (2.3 #: null #: JsonNil) #: JsonNil
        tuple3.filter(_ !== (2.3 #: null #: JsonNil)) shouldBe "hello" #: true #: JsonNil
      }
    }

    describe("when mapping some elements") {
      it("should map every element in the tuple") {
        val tuple = 2.3 #: 4.5f #: JsonNil

        tuple0.map(_ => null) shouldBe JsonNil
        tuple0.map(_ => JsonNil) shouldBe JsonNil
        tuple0.map(_ => tuple) shouldBe JsonNil
        tuple1.map(_ => null) shouldBe null #: JsonNil
        tuple1.map(_ => JsonNil) shouldBe JsonNil #: JsonNil
        tuple1.map(_ => tuple) shouldBe tuple #: JsonNil
        tuple3.map(_ => null) shouldBe null #: null #: null #: JsonNil
        tuple3.map(_ => JsonNil) shouldBe JsonNil #: JsonNil #: JsonNil #: JsonNil
        tuple3.map(_ => tuple) shouldBe tuple #: tuple #: tuple #: JsonNil
      }
    }

    describe("when flatmapping some elements") {
      it("should map every element in the tuple and then flatten it") {
        val tuple = 2.3 #: 4.5f #: JsonNil

        tuple0.flatMap(_ => JsonNil) shouldBe JsonNil
        tuple0.flatMap(_ => tuple) shouldBe JsonNil
        tuple1.flatMap(_ => JsonNil) shouldBe JsonNil
        tuple1.flatMap(_ => tuple) shouldBe tuple
        tuple3.flatMap(_ => JsonNil) shouldBe JsonNil
        tuple3.flatMap(_ => tuple) shouldBe 2.3 #: 4.5f #: 2.3 #: 4.5f #: 2.3 #: 4.5f #: JsonNil
      }
    }

    describe("when folding left over some elements") {
      it("should aggregate all elements starting from the leftmost and going to the right") {
        tuple0.foldLeft("")(_ + " " + _.toString) shouldBe empty
        tuple0.foldLeft(0)((c, _) => c + 1) shouldBe 0
        tuple1.foldLeft("")(_ + " " + _.toString) shouldBe " 0"
        tuple1.foldLeft(0)((c, _) => c + 1) shouldBe 1
        tuple3.foldLeft("")(_ + " " + _.toString) shouldBe " hello true (2.3, null)"
        tuple3.foldLeft(0)((c, _) => c + 1) shouldBe 3
      }
    }

    describe("when folding right over some elements") {
      it("should aggregate all elements starting from the rightmost and going to the left") {
        tuple0.foldRight("")((e, s) => s + " " + e.toString) shouldBe empty
        tuple0.foldRight(0)((_, c) => c + 1) shouldBe 0
        tuple1.foldRight("")((e, s) => s + " " + e.toString) shouldBe " 0"
        tuple1.foldRight(0)((_, c) => c + 1) shouldBe 1
        tuple3.foldRight("")((e, s) => s + " " + e.toString) shouldBe " (2.3, null) true hello"
        tuple3.foldRight(0)((_, c) => c + 1) shouldBe 3
      }
    }

    describe("when taking its head") {
      it("should return it if the tuple is not empty") {
        tuple0.head shouldBe empty
        tuple1.head.value shouldBe 0
        tuple3.head.value shouldBe "hello"
      }
    }

    describe("when taking its tail") {
      it("should return it if the tuple has at least two elements") {
        tuple0.tail shouldBe JsonNil
        tuple1.tail shouldBe JsonNil
        tuple3.tail shouldBe true #: (2.3 #: null #: JsonNil) #: JsonNil
      }
    }

    describe("when taking its initial portion") {
      it("should return it if the tuple has at least two elements") {
        tuple0.init shouldBe JsonNil
        tuple1.init shouldBe JsonNil
        tuple3.init shouldBe "hello" #: true #: JsonNil
      }
    }

    describe("when taking its last element") {
      it("should return it if the tuple has at least one elements") {
        tuple0.last shouldBe empty
        tuple1.last.value shouldBe 0
        tuple3.last.value shouldBe 2.3 #: null #: JsonNil
      }
    }

    describe("when splitting in two json tuples") {
      it("should return a tuple made by the two halves") {
        tuple0.split(-1) shouldBe JsonNil
        tuple0.split(0) shouldBe JsonNil #: JsonNil #: JsonNil
        tuple0.split(1) shouldBe JsonNil #: JsonNil #: JsonNil
        tuple0.split(3) shouldBe JsonNil #: JsonNil #: JsonNil
        tuple1.split(-1) shouldBe tuple1
        tuple1.split(0) shouldBe JsonNil #: tuple1 #: JsonNil
        tuple1.split(1) shouldBe tuple1 #: JsonNil #: JsonNil
        tuple1.split(3) shouldBe tuple1 #: JsonNil #: JsonNil
        tuple3.split(-1) shouldBe tuple3
        tuple3.split(0) shouldBe JsonNil #: tuple3 #: JsonNil
        tuple3.split(1) shouldBe ("hello" #: JsonNil) #: (true #: (2.3 #: null #: JsonNil) #: JsonNil) #: JsonNil
        tuple3.split(3) shouldBe ("hello" #: true #: (2.3 #: null #: JsonNil) #: JsonNil) #: JsonNil #: JsonNil
      }
    }

    describe("when taking some elements") {
      it("should take the specified elements") {
        tuple0.take(-1) shouldBe JsonNil
        tuple0.take(0) shouldBe JsonNil
        tuple0.take(1) shouldBe JsonNil
        tuple0.take(5) shouldBe JsonNil
        tuple1.take(-1) shouldBe tuple1
        tuple1.take(0) shouldBe JsonNil
        tuple1.take(1) shouldBe tuple1
        tuple1.take(5) shouldBe tuple1
        tuple3.take(-1) shouldBe tuple3
        tuple3.take(0) shouldBe JsonNil
        tuple3.take(1) shouldBe "hello" #: JsonNil
        tuple3.take(5) shouldBe tuple3
      }
    }

    describe("when zipping another tuple") {
      it("should zip the elements of the two tuples two by two creating a tuple of two-elements tuples") {
        tuple0.zip(JsonNil) shouldBe JsonNil
        tuple0.zip(tuple0) shouldBe JsonNil
        tuple0.zip(tuple1) shouldBe JsonNil
        tuple0.zip(tuple3) shouldBe JsonNil
        tuple1.zip(JsonNil) shouldBe JsonNil
        tuple1.zip(tuple0) shouldBe JsonNil
        tuple1.zip(tuple1) shouldBe (0 #: 0 #: JsonNil) #: JsonNil
        tuple1.zip(tuple3) shouldBe (0 #: "hello" #: JsonNil) #: JsonNil
        tuple3.zip(JsonNil) shouldBe JsonNil
        tuple3.zip(tuple0) shouldBe JsonNil
        tuple3.zip(tuple1) shouldBe ("hello" #: 0 #: JsonNil) #: JsonNil
        tuple3.zip(tuple3) shouldBe JsonTuple(
          JsonTuple("hello", "hello"),
          JsonTuple(true, true),
          JsonTuple(JsonTuple(2.3, null), JsonTuple(2.3, null))
        )
      }
    }

    describe("when transformed into a Seq") {
      it("should put all its elements into the Seq") {
        tuple0.toSeq shouldBe Seq.empty[JsonElement]
        tuple1.toSeq shouldBe Seq(0)
        tuple3.toSeq shouldBe Seq("hello", true, JsonTuple(2.3, null))
      }
    }

    describe("when executing an action for each element") {
      it("should apply it for each one of them") {
        var x = 0

        tuple0.foreach(_ => x += 1)
        x shouldBe 0
        x = 0
        tuple1.foreach(_ => x += 1)
        x shouldBe 1
        x = 0
        tuple3.foreach(_ => x += 1)
        x shouldBe 3
      }
    }
  }
}
