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

import scala.annotation.tailrec
import scala.annotation.targetName
import scala.util.Try

/** A tuple, an object representing an immutable and ordered sequence of values of different types.
  *
  * This implementation of a tuple is a "JSON" tuple, meaning that it is built on the fundamental concepts of the JSON format,
  * enabling an easier exchange of data over the wire. This is because JSON is now the de-facto format for the data exchange on
  * the web, which in turn is the standard for building distributed architectures, namely webservices. This means that a JSON
  * tuple is the same concept as a JSON array, so an indexed sequence of values allowed in JSON, minus the "JSON object". Objects
  * are not allowed since this would violate the rigid structure of a tuple: this would allow to objects to nest tuples inside
  * them, which could nest other objects, which in turn could nest other tuples and so on and so forth. This would not correctly
  * represent what in most programming languages is a sequence or list of elements. However, tuples can be nested into other
  * tuples, enabling the equivalent of a matrix or of a jagged array. Their usefulness is in representing simple, not deeply
  * nested data structures, such as messages or events exchanged in a distributed system.
  */
sealed trait JsonTuple {

  /** The constructor method for a JSON tuple, creating a new tuple from this tuple prepending a new head element to it. Being an
    * operator ending with ":", this method is right associative, allowing a syntax such as
    *
    * <code>val tuple = 0 #: true #: "hello" #: JsonNil</code>
    *
    * @param h
    *   The new element to be prepended to this tuple, for creating a new tuple
    * @return
    *   a new tuple with the new element prepended to this tuple
    */
  @targetName("cons")
  infix def #:(h: JsonElement): JsonTuple
}

/** Companion object to the [[JsonTuple]] trait, containing its factory methods and its implementations. */
@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
object JsonTuple {

  /** An empty JSON tuple, which could be seen also as a terminator for a JSON tuple definition.
    *
    * For all data structures recursively defined, this represents the "base case" of the recursive definition, the empty tuple
    * which signals that no more elements needs to be expected. So it has a meaning used on its own, representing an empty tuple,
    * but also can be used for ending a definition of a tuple with a syntax such as
    *
    * <code>val tuple = 0 #: true #: "hello" #: JsonNil</code>
    */
  case object JsonNil extends JsonTuple {

      /** The constructor method for a JSON tuple, creating a new tuple from this tuple prepending a new head element to it. Being
        * an operator ending with ":", this method is right associative, allowing a syntax such as
        *
        * <code>val tuple = 0 #: true #: "hello" #: JsonNil</code>
        *
        * @param h
        *   the new element to be prepended to this tuple, for creating a new tuple
        * @return
        *   a new tuple with the new element prepended to this tuple
        */
    @targetName("cons")
    override infix def #:(h: JsonElement): JsonTuple = new #:(h, JsonNil)

    override def toString: String = "()"
  }

  /** A non-empty JSON tuple, representing a generic tuple containing at least one element.
    *
    * This implementation represents a tuple with at least one element, which is built element by element exploiting the
    * "constructor" operator. Because the JSON tuple is a recursively defined data structure, the at-least-one defined element
    * must be the one in its head, while its tail can be a [[JsonNil]], if the tuple contains exactly one element, or another
    * non-empty JSON tuple otherwise. This enables the recursion of this data structure.
    *
    * @param h
    *   the head of this non-empty JSON tuple, which is its first element
    * @param t
    *   the tail of this non-empty JSON tuple, which contains all the remaining elements in order
    */
  @targetName("JsonNonEmptyTuple")
  final case class #:(h: JsonElement, t: JsonTuple) extends JsonTuple {

      /** The constructor method for a JSON tuple, creating a new tuple from this tuple prepending a new head element to it. Being
        * an operator ending with ":", this method is right associative, allowing a syntax such as
        *
        * <code>val tuple = 0 #: true #: "hello" #: JsonNil</code>
        *
        * @param h
        *   The new element to be prepended to this tuple, for creating a new tuple
        * @return
        *   a new tuple with the new element prepended to this tuple
        */
    @targetName("cons")
    override infix def #:(h: JsonElement): JsonTuple = new #:(h, this)

    override def toString: String = {
      @tailrec
      def _toString(t: JsonTuple, acc: String): String = t match {
        case x #: xs => _toString(xs, acc + ", " + String.valueOf(x))
        case _ => acc
      }

      "(" + String.valueOf(h) + _toString(t, acc = "") + ")"
    }
  }

  /** Creates a new empty JSON tuple. */
  def apply(): JsonTuple = JsonNil

  /** Creates a new non-empty JSON tuple consisting of only one element.
    *
    * @param v
    *   the element which is the one and only making this JSON tuple
    * @return
    *   a new non-empty JSON tuple made only of one element
    */
  def apply(v: JsonElement): JsonTuple = v #: JsonNil

  /** Creates a new JSON tuple given the variable number of elements passed to it. If no elements are passed, an empty JSON tuple
    * is created, a non-empty JSON tuple otherwise.
    *
    * @param vs
    *   the variable number of elements to be used for creating a new JSON tuple
    * @return
    *   a new JSON tuple consisting of the given elements, in the same order they were passed
    */
  def apply(vs: JsonElement*): JsonTuple = fromSeq(vs)

  /** Creates a new JSON tuple from a [[Seq]], keeping the same order of the elements as in the [[Seq]]. If an empty sequence is
    * given, an empty tuple will be returned, a non-empty JSON tuple otherwise.
    *
    * @param vs
    *   the [[Seq]] which elements are to be used for building a new JSON tuple
    * @return
    *   a new JSON tuple consisting of the same elements of the given [[Seq]] in the same order
    */
  def fromSeq(vs: Seq[JsonElement]): JsonTuple = {
    @tailrec
    def _fromSeq(t: JsonTuple, s: Seq[JsonElement]): JsonTuple = s match {
      case x +: xs => _fromSeq(x #: t, xs)
      case _ => t
    }
    _fromSeq(JsonNil, vs.reverse)
  }
}

export JsonTuple.{JsonNil, #:}
