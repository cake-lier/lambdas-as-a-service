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

/** A type-class representing the operations that must be supported by a tuple-like object.
  *
  * Objects representing tuples, being so, must provide some operations that are expected to work on them. A tuple must allow to
  * peek its internal structure in a predictable way, giving access to its number of elements, the elements at any given position,
  * its head or its tail, to the initial part of it or the ending element. A tuple must also provide operations to update it,
  * returning a new tuple, in a similar fashion to a sequence. So, elements can be appended, dropped, filtered, mapped,
  * flat-mapped, taken, folded or an action can be performed for each one of them. At last, a tuple can be concatenated to another
  * tuple, it can be converted to a Seq, existing a bijective relationship between them, split into two or zipped with another.
  *
  * @tparam T
  *   the concrete type of tuple that must support these operations
  * @tparam E
  *   the concrete type of an element in the tuple
  * @tparam G
  *   the abstract type of tuple to which the concrete type belongs
  */
trait TupleOps[-T <: G, E, G] {

  /** Returns the arity, i.e. the number of elements of the tuple. An empty tuple returns an arity of 0.
    *
    * @param t
    *   the tuple for which calculating the arity
    * @return
    *   the arity of the tuple
    */
  def arity(t: T): Int

  /** Appends an element to the end of the tuple, making it grow one element from the end. If an empty tuple is used for appending
    * an element, the resulting tuple has only one element which is also its head.
    *
    * @param t
    *   the tuple to which appending an element
    * @param v
    *   the element to be appended
    * @return
    *   a new tuple with the given element appended to it
    */
  @targetName("append")
  infix def :#(t: T, v: E): G

  /** Concatenates the second tuple after the first, with the resulting tuple having as arity the sum of both of them. If either
    * of the tuples is an empty tuple, this operation returns the other, as concatenating an empty tuple has no effect.
    *
    * @param t1
    *   the tuple to which concatenate the second
    * @param t2
    *   the tuple to be concatenated to the first
    * @return
    *   a new tuple with the second one added to the end of the first one
    */
  @targetName("concat")
  def :##(t1: T, t2: G): G

  /** Drops the first elements of the given tuple in a given number, returning a tuple with the same elements as the given one,
    * except the ones at the beginning. If the given value exceeds the arity of the tuple or it is a negative number, no operation
    * is performed and the given tuple is returned unchanged.
    *
    * @param t
    *   the tuple from which dropping the first elements
    * @param n
    *   the number of elements to be dropped from the beginning
    * @return
    *   the given tuple with the first elements at the beginning dropped, if possible, the unchanged tuple otherwise
    */
  def drop(t: T, n: Int): G

  /** Returns the element at the n-th position in the tuple, if one exists. If no element exists, either because the position
    * exceeds the arity of the tuple or it is a negative number, a [[None]] is returned.
    *
    * @param t
    *   the tuple from which extracting an element
    * @param n
    *   the position from which extracting an element
    * @return
    *   a [[Some]] containing the element at the n-th position in the tuple if it exists, a [[None]] otherwise
    */
  def elem(t: T, n: Int): Option[E]

  /** Returns a copy of the given tuple where only the elements passing the given predicate used as filter are kept. If all
    * elements make the predicate return <code>true</code>, the original tuple is returned. If no element make the predicate
    * return <code>true</code>, an empty tuple is returned.
    *
    * @param t
    *   the tuple on which applying the given filter
    * @param p
    *   the predicate to be used as filter
    * @return
    *   a tuple with only the elements of the given one passing the predicate used as filter
    */
  def filter(t: T, p: E => Boolean): G

  /** Returns a tuple where the given function has been applied for every element in the given tuple. This operation maps elements
    * of the tuple into elements of the tuple, which means that if the function returns a tuple, this is not concatenated, but
    * inserted into the result as any other element.
    *
    * @param t
    *   the tuple on which applying the given function on each element
    * @param f
    *   the function to be applied on each element
    * @return
    *   a tuple where each element in it has been transformed according to the given function
    */
  def map(t: T, f: E => E): G

  /** Returns a tuple where the given function has been applied for every element in the given tuple. This operation maps elements
    * of the tuple into tuples, so a flattening operation, or concatenation, is then performed so as to obtain a tuple as a
    * result. The flattening happens only on one level, so if a tuple containing a tuple is returned by the function, the inner
    * tuple will be an element of the resulting tuple.
    *
    * @param t
    *   the tuple on which applying the given function on each element
    * @param f
    *   the function to be applied on each element
    * @return
    *   a tuple where each element in it has been transformed according to the given function
    */
  def flatMap(t: T, f: E => G): G

  /** Returns an element which is the result of accumulating all elements of the given tuple from left to right, using the given
    * element as a starting point and the given function as the accumulator. Being so, the accumulator will specify how to combine
    * the elements folded so far into a single value with a new element of the tuple. If the given tuple is empty the starting
    * element will be returned.
    *
    * @param t
    *   the tuple on which applying the folding operation
    * @param z
    *   the starting element of the folding operation
    * @param a
    *   the accumulation function
    * @tparam A
    *   the type of the resulting element of this function
    * @return
    *   all elements in the given tuple folded into one, starting from the given element and combining them once at a time using
    *   the given accumulation function
    */
  def foldLeft[A](t: T, z: A, a: (A, E) => A): A

  /** Returns an element which is the result of accumulating all elements of the given tuple from right to left, using the given
    * element as a starting point and the given function as the accumulator. Being so, the accumulator will specify how to combine
    * the elements folded so far into a single value with a new element of the tuple. If the given tuple is empty the starting
    * element will be returned.
    *
    * @param t
    *   the tuple on which applying the folding operation
    * @param z
    *   the starting element of the folding operation
    * @param a
    *   the accumulation function
    * @tparam A
    *   the type of the resulting element of this function
    * @return
    *   all elements in the given tuple folded into one, starting from the given element and combining them once at a time using
    *   the given accumulation function
    */
  def foldRight[A](t: T, z: A, a: (E, A) => A): A

  /** Returns the head of the given tuple, so its first element. If the tuple is empty, a [[None]] is returned.
    *
    * @param t
    *   the tuple from which getting its head
    * @return
    *   a [[Some]] containing the head of the given tuple, if it exists, a [[None]] otherwise
    */
  def head(t: T): Option[E]

  /** Returns the tail of the given tuple, so all elements excluding its first one. If the tuple is empty, its tail is also empty,
    * so an empty tuple is returned.
    *
    * @param t
    *   the tuple from which getting its tail
    * @return
    *   the tail of the given tuple
    */
  def tail(t: T): G

  /** Returns the initial part of the given tuple, so all its elements except its last. If the tuple is empty or contains only one
    * element, its tail is also empty, so an empty tuple is returned.
    *
    * @param t
    *   the tuple from which getting its initial part
    * @return
    *   the initial part of the given tuple
    */
  def init(t: T): G

  /** Returns the last element of the given tuple, if it exists. If the tuple is empty a [[None]] is returned.
    *
    * @param t
    *   the tuple from which getting its last element
    * @return
    *   a [[Some]] containing the last element of the given tuple, if it exists, a [[None]] otherwise
    */
  def last(t: T): Option[E]

  /** Splits the given tuple into two at the given index, returning a tuple made of two tuples, which concatenated make the
    * original tuple. The splitting is always made to include the element at the given index in the second tuple, so an index of
    * <code>0</code> will always return a tuple made of an empty tuple followed by the original tuple. If the given index is
    * negative, the original tuple is returned. If an empty tuple is given to split, a tuple made of two empty tuples is then
    * returned.
    *
    * @param t
    *   the tuple to split
    * @param n
    *   the index at which splitting the tuple
    * @return
    *   a tuple made of two tuples, which concatenated will yield the original tuple
    */
  def split(t: T, n: Int): G

  /** Returns the given tuple with only the first given number of elements kept, while the ones exceeding the count are dropped.
    * If a value of <code>0</code> is supplied, an empty tuple is returned. If a negative value is supplied, the original tuple is
    * returned.
    *
    * @param t
    *   the tuple from which taking its first elements
    * @param n
    *   the number of elements to take
    * @return
    *   the given tuple with only the first given number of elements kept
    */
  def take(t: T, n: Int): G

  /** Returns a tuple which elements are tuples made by an element of the given first tuple and an element of the given second
    * tuple. The elements are coupled one by one by their corresponding positions. This means that, if one tuple has an arity
    * greater than the other, the exceeding elements are dropped and not coupled. This means also that if one of the two given
    * tuples is an empty tuple, then the returned tuple is an empty tuple.
    *
    * @param t1
    *   the first tuple to be zipped
    * @param t2
    *   the second tuple to be zipped
    * @return
    *   a tuple which elements are tuples which couple the corresponding elements of the two given tuples
    */
  def zip(t1: T, t2: G): G

  /** Returns the given tuple as a [[Seq]], keeping the elements in the same order as in the tuple. If the given tuple is an empty
    * tuple, an empty [[Seq]] is returned.
    *
    * @param t
    *   the tuple to be converted into a [[Seq]]
    * @return
    *   a [[Seq]] containing the same elements of the tuple in the same order
    */
  def toSeq(t: T): Seq[E]

  /** Executes the given action for each element of the given tuple, allowing for side-effect-ful operations on the tuple. If the
    * given tuple is an empty tuple, the action is performed zero times.
    *
    * @param t
    *   the tuple on which executing the action for each element
    * @param f
    *   the action to be performed
    */
  def foreach(t: T, f: E => Unit): Unit
}

/** Companion object to the [[TupleOps]] type-class, containing the type-class interface and the implementation for the
  * [[JsonTuple]] type.
  */
object TupleOps {

  /** Type-class interface for the [[TupleOps]] type-class. */
  extension [T <: G, E, G](t1: T)(using TupleOps[T, E, G]) {

    /** Returns the arity, i.e. the number of elements of this tuple. An empty tuple returns an arity of 0. */
    def arity: Int = implicitly[TupleOps[T, E, G]].arity(t1)

    /** Appends an element to the end of this tuple, making it grow one element from the end. If an empty tuple is used for
      * appending an element, the resulting tuple has only one element which is also its head.
      *
      * @param v
      *   the element to be appended
      * @return
      *   a new tuple with the given element appended to it
      */
    @targetName("append")
    infix def :#(v: E): G = implicitly[TupleOps[T, E, G]].:#(t1, v)

    /** Concatenates the given tuple after this one, with the resulting tuple having as arity the sum of both of them. If either
      * of the tuples is an empty tuple, this operation returns the other, as concatenating an empty tuple has no effect.
      *
      * @param t
      *   the tuple to be concatenated
      * @return
      *   a new tuple with the given one added to the end of this one
      */
    @targetName("concat")
    def :##(t: G): G = implicitly[TupleOps[T, E, G]].:##(t1, t)

    /** Drops the first elements of this tuple in a given number, returning a tuple with the same elements as this, except the
      * ones at the beginning. If the given value exceeds the arity of the tuple or it is a negative number, no operation is
      * performed and this tuple is returned unchanged.
      *
      * @param n
      *   the number of elements to be dropped from the beginning
      * @return
      *   this tuple with the first elements at the beginning dropped, if possible, the unchanged tuple otherwise
      */
    def drop(n: Int): G = implicitly[TupleOps[T, E, G]].drop(t1, n)

    /** Returns the element at the n-th position in this tuple, if one exists. If no element exists, either because the position
      * exceeds the arity of the tuple or it is a negative number, a [[None]] is returned.
      *
      * @param n
      *   the position from which extracting an element
      * @return
      *   a [[Some]] containing the element at the n-th position in this tuple if it exists, a [[None]] otherwise
      */
    def elem(n: Int): Option[E] = implicitly[TupleOps[T, E, G]].elem(t1, n)

    /** Returns a copy of this tuple where only the elements passing the given predicate used as filter are kept. If all elements
      * make the predicate return <code>true</code>, this tuple is returned. If no element make the predicate return
      * <code>true</code>, an empty tuple is returned.
      *
      * @param p
      *   the predicate to be used as filter
      * @return
      *   a tuple with only the elements from this one passing the predicate used as filter
      */
    def filter(p: E => Boolean): G = implicitly[TupleOps[T, E, G]].filter(t1, p)

    /** Returns a tuple where the given function has been applied for every element in this tuple. This operation maps elements of
      * the tuple into elements of the tuple, which means that if the function returns a tuple, this is not concatenated, but
      * inserted into the result as any other element.
      *
      * @param f
      *   the function to be applied on each element of this tuple
      * @return
      *   a tuple where each element in it has been transformed according to the given function
      */
    def map(f: E => E): G = implicitly[TupleOps[T, E, G]].map(t1, f)

    /** Returns a tuple where the given function has been applied for every element in this tuple. This operation maps elements of
      * the tuple into tuples, so a flattening operation, or concatenation, is then performed so as to obtain a tuple as a result.
      * The flattening happens only on one level, so if a tuple containing a tuple is returned by the function, the inner tuple
      * will be an element of the resulting tuple.
      *
      * @param f
      *   the function to be applied on each element of this tuple
      * @return
      *   a tuple where each element in it has been transformed according to the given function
      */
    def flatMap(f: E => G): G = implicitly[TupleOps[T, E, G]].flatMap(t1, f)

    /** Returns an element which is the result of accumulating all elements of this tuple from left to right, using the given
      * element as a starting point and the given function as the accumulator. Being so, the accumulator will specify how to
      * combine the elements folded so far into a single value with a new element of this tuple. If this tuple is empty the
      * starting element will be returned.
      *
      * @param z
      *   the starting element of the folding operation
      * @param a
      *   the accumulation function
      * @tparam A
      *   the type of the resulting element of this function
      * @return
      *   all elements in this tuple folded into one, starting from the given element and combining them once at a time using the
      *   given accumulation function
      */
    def foldLeft[A](z: A)(a: (A, E) => A): A = implicitly[TupleOps[T, E, G]].foldLeft(t1, z, a)

    /** Returns an element which is the result of accumulating all elements of this tuple from right to left, using the given
      * element as a starting point and the given function as the accumulator. Being so, the accumulator will specify how to
      * combine the elements folded so far into a single value with a new element of this tuple. If this tuple is empty the
      * starting element will be returned.
      *
      * @param z
      *   the starting element of the folding operation
      * @param a
      *   the accumulation function
      * @tparam A
      *   the type of the resulting element of this function
      * @return
      *   all elements in this tuple folded into one, starting from the given element and combining them once at a time using the
      *   given accumulation function
      */
    def foldRight[A](z: A)(a: (E, A) => A): A = implicitly[TupleOps[T, E, G]].foldRight(t1, z, a)

    /** Returns the head of the given tuple, so its first element. If the tuple is empty, a [[None]] is returned. */
    def head: Option[E] = implicitly[TupleOps[T, E, G]].head(t1)

    /** Returns the tail of the given tuple, so all elements excluding its first one. If the tuple is empty, its tail is also
      * empty, so an empty tuple is returned.
      */
    def tail: G = implicitly[TupleOps[T, E, G]].tail(t1)

    /** Returns the initial part of the given tuple, so all its elements except its last. If the tuple is empty or contains only
      * one element, its tail is also empty, so an empty tuple is returned.
      */
    def init: G = implicitly[TupleOps[T, E, G]].init(t1)

    /** Returns the last element of the given tuple, if it exists. If the tuple is empty a [[None]] is returned. */
    def last: Option[E] = implicitly[TupleOps[T, E, G]].last(t1)

    /** Splits this tuple into two at the given index, returning a tuple made of two tuples, which concatenated make this tuple.
      * The splitting is always made to include the element at the given index in the second tuple, so an index of <code>0</code>
      * will always return a tuple made of an empty tuple followed by this tuple. If the given index is negative, this tuple is
      * returned. If an empty tuple is given to split, a tuple made of two empty tuples is then returned.
      *
      * @param n
      *   the index at which splitting the tuple
      * @return
      *   a tuple made of two tuples, which concatenated will yield this tuple
      */
    def split(n: Int): G = implicitly[TupleOps[T, E, G]].split(t1, n)

    /** Returns this tuple with only the first elements kept in a number which is given, while the ones exceeding the count are
      * dropped. If a value of <code>0</code> is supplied, an empty tuple is returned. If a negative value is supplied, this tuple
      * is returned.
      *
      * @param n
      *   the number of elements to take
      * @return
      *   this tuple with only the first given number of elements kept
      */
    def take(n: Int): G = implicitly[TupleOps[T, E, G]].take(t1, n)

    /** Returns a tuple which elements are tuples made by an element of this tuple and an element of the given tuple. The elements
      * are coupled one by one by their corresponding positions. This means that, if one tuple has an arity greater than the
      * other, the exceeding elements are dropped and not coupled. This means also that if one of the two tuples is an empty
      * tuple, then the returned tuple is an empty tuple.
      *
      * @param t
      *   the other tuple to be zipped
      * @return
      *   a tuple which elements are tuples which couple the corresponding elements of this tuple and the given tuple
      */
    def zip(t: G): G = implicitly[TupleOps[T, E, G]].zip(t1, t)

    /** Returns this tuple as a [[Seq]], keeping the elements in the same order as in the tuple. If this tuple is an empty tuple,
      * an empty [[Seq]] is returned.
      */
    def toSeq: Seq[E] = implicitly[TupleOps[T, E, G]].toSeq(t1)

    /** Executes the given action for each element of this tuple, allowing for side-effect-ful operations on the tuple. If this
      * tuple is an empty tuple, the action is performed zero times.
      *
      * @param f
      *   the action to be performed on each element of this tuple
      */
    def foreach(f: E => Unit): Unit = implicitly[TupleOps[T, E, G]].foreach(t1, f)
  }

  /** The implementation of the [[TupleOps]] type-class for the [[JsonTuple]] type and its element type, [[JsonElement]]. */
  given [T <: JsonTuple]: TupleOps[T, JsonElement, JsonTuple] with {

    override def arity(t: T): Int = {
      @tailrec
      def _arity(r: JsonTuple, acc: Int): Int = r match {
        case _ #: xs => _arity(xs, acc + 1)
        case _ => acc
      }
      _arity(t, acc = 0)
    }

    @targetName("append")
    override infix def :#(t: T, v: JsonElement): JsonTuple = {
      @tailrec
      def _append(r: JsonTuple, acc: JsonTuple => JsonTuple): JsonTuple = r match {
        case x #: xs => _append(xs, e => acc(x #: e))
        case _ => acc(v #: JsonNil)
      }
      _append(t, identity[JsonTuple])
    }

    /* Effective implementation of the concat method, reified for correct signature. */
    private def reifiedConcat(t1: JsonTuple, t2: JsonTuple): JsonTuple = {
      @tailrec
      def _concat(r: JsonTuple, acc: JsonTuple => JsonTuple): JsonTuple = r match {
        case x #: xs => _concat(xs, e => acc(x #: e))
        case _ => acc(t2)
      }
      _concat(t1, identity[JsonTuple])
    }

    @targetName("concat")
    override infix def :##(t1: T, t2: JsonTuple): JsonTuple = reifiedConcat(t1, t2)

    override def drop(t: T, n: Int): JsonTuple = {
      @tailrec
      def _drop(r: JsonTuple, c: Int): JsonTuple = c match {
        case 0 => r
        case _ =>
          r match {
            case _ #: xs => _drop(xs, c - 1)
            case _ => JsonNil
          }
      }
      if (n >= 0) _drop(t, n) else t
    }

    override def elem(t: T, n: Int): Option[JsonElement] = {
      @tailrec
      def _elem(r: JsonTuple, c: Int): Option[JsonElement] = r match {
        case x #: xs =>
          c match {
            case 0 => Some(x)
            case _ => _elem(xs, c - 1)
          }
        case _ => None
      }

      if (n >= 0) _elem(t, n) else None
    }

    override def filter(t: T, p: JsonElement => Boolean): JsonTuple = {
      @tailrec
      def _filter(r: JsonTuple, acc: JsonTuple => JsonTuple): JsonTuple = r match {
        case x #: xs if p(x) => _filter(xs, e => acc(x #: e))
        case _ #: xs => _filter(xs, acc)
        case _ => acc(JsonNil)
      }

      _filter(t, identity[JsonTuple])
    }

    override def map(t: T, f: JsonElement => JsonElement): JsonTuple = {
      @tailrec
      def _map(r: JsonTuple, acc: JsonTuple => JsonTuple): JsonTuple = r match {
        case x #: xs => _map(xs, e => acc(f(x) #: e))
        case _ => acc(JsonNil)
      }

      _map(t, identity[JsonTuple])
    }

    override def flatMap(t: T, f: JsonElement => JsonTuple): JsonTuple = {
      @tailrec
      def _flatMap(r: JsonTuple, acc: JsonTuple => JsonTuple): JsonTuple = r match {
        case x #: xs => _flatMap(xs, e => acc(reifiedConcat(f(x), e)))
        case _ => acc(JsonNil)
      }

      _flatMap(t, identity[JsonTuple])
    }

    override def foldLeft[A](t: T, z: A, a: (A, JsonElement) => A): A = {
      @tailrec
      def _foldLeft(r: JsonTuple, acc: A): A = r match {
        case x #: xs => _foldLeft(xs, a(acc, x))
        case _ => acc
      }

      _foldLeft(t, z)
    }

    override def foldRight[A](t: T, z: A, a: (JsonElement, A) => A): A = {
      @tailrec
      def _foldRight(r: JsonTuple, acc: A => A): A = r match {
        case x #: xs => _foldRight(xs, e => acc(a(x, e)))
        case _ => acc(z)
      }

      _foldRight(t, identity[A])
    }

    override def head(t: T): Option[JsonElement] = t match {
      case x #: _ => Some(x)
      case _ => None
    }

    override def tail(t: T): JsonTuple = t match {
      case _ #: xs => xs
      case _ => JsonNil
    }

    override def init(t: T): JsonTuple = {
      @tailrec
      def _init(r: JsonTuple, acc: JsonTuple => JsonTuple): JsonTuple = r match {
        case _ #: JsonNil | JsonNil => acc(JsonNil)
        case x #: xs => _init(xs, e => acc(x #: e))
      }

      _init(t, identity[JsonTuple])
    }

    override def last(t: T): Option[JsonElement] = {
      @tailrec
      def _last(r: JsonTuple): Option[JsonElement] = r match {
        case x #: JsonNil => Some(x)
        case _ #: xs => _last(xs)
        case _ => None
      }

      _last(t)
    }

    override def split(t: T, n: Int): JsonTuple = if (n >= 0) JsonTuple(take(t, n), drop(t, n)) else t

    override def take(t: T, n: Int): JsonTuple = {
      @tailrec
      def _take(r: JsonTuple, v: Int, acc: JsonTuple => JsonTuple): JsonTuple = v match {
        case 0 => acc(JsonNil)
        case _ =>
          r match {
            case x #: xs => _take(xs, v - 1, e => acc(x #: e))
            case _ => acc(JsonNil)
          }
      }

      if (n >= 0) _take(t, n, identity[JsonTuple]) else t
    }

    override def zip(t1: T, t2: JsonTuple): JsonTuple = {
      @tailrec
      def _zip(r1: JsonTuple, r2: JsonTuple, acc: JsonTuple => JsonTuple): JsonTuple = (r1, r2) match {
        case (x1 #: xs1, x2 #: xs2) => _zip(xs1, xs2, e => acc(JsonTuple(x1, x2) #: e))
        case (_, JsonNil) | (JsonNil, _) => acc(JsonNil)
      }

      _zip(t1, t2, identity[JsonTuple])
    }

    override def toSeq(t: T): Seq[JsonElement] = {
      @tailrec
      def _toSeq(r: JsonTuple, s: Seq[JsonElement]): Seq[JsonElement] = r match {
        case x #: xs => _toSeq(xs, s :+ x)
        case _ => s
      }

      _toSeq(t, Seq.empty[JsonElement])
    }

    override def foreach(t: T, f: JsonElement => Unit): Unit = {
      @tailrec
      def _foreach(r: JsonTuple): Unit = r match {
        case x #: xs =>
          f(x)
          _foreach(xs)
        case _ => ()
      }

      _foreach(t)
    }
  }
}

export TupleOps.*
