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

/** A JSON element, an admissible element in the JSON specification.
  *
  * This type represents an element in the JSON specification, such as an integer number, a long integer number, a floating point
  * single precision number, a floating point double precision number, a boolean value, a string value, a null value or a
  * [[JsonTuple]]. This last type is needed because not only this type is of a JSON element, it is also the type of all elements
  * that can be contained into a [[JsonTuple]]. In this way, the [[JsonTuple]]s can be nested one inside the other, because an
  * element in a tuple can be another tuple. Being so, a JSON element can also be a JSON array, because a JSON tuple is just that,
  * but not a JSON object. Objects are not allowed since this would violate the rigid structure of a tuple: this would allow to
  * objects to nest tuples inside them, which could nest other objects, which in turn could nest other tuples and so on and so
  * forth. This would not correctly represent what in most programming languages is a sequence or list of elements.
  */
type JsonElement = Int | Long | Double | Float | Boolean | String | JsonTuple | Null
