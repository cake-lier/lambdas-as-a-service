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
package laas.tuplespace.dsl.numeric

import laas.tuplespace.*

/** The object implementing the [[NumericTemplate]] trait for the float type.
  *
  * This object represents the specific implementation of the family of traits defined by the [[NumericTemplate]] one. It then
  * implements them all specifying that the generic numeric type that is specified in all of them is the [[Float]] one.
  */
object FloatTemplate extends NumericTemplate {

    /** The "empty" float template, capable of matching any value of the float type.
      *
      * This is the starting template from which creating any other float template by specifying more and more constraints. Being
      * so, it is allowed to add a minimum value, either inclusive or exclusive, or a maximum value, either inclusive or
      * exclusive, to create a new template from this.
      */
  class EmptyFloatTemplate
    extends EmptyNumericTemplate[
      Float,
      WithMinimumFloatTemplate,
      WithMaximumFloatTemplate,
      CompleteFloatTemplate
    ](
      new WithMinimumFloatTemplate(_),
      new WithMaximumFloatTemplate(_),
      () => JsonFloatTemplate(None, None, None, None, None)
    )

    /** The float template for which the minimum value, either inclusive or exclusive, has been specified.
      *
      * This template represents one for float values where the minimum constraint, whether it be an inclusive or exclusive one,
      * has been specified. Now, only a maximum constraint can be specified, closing the now half-open interval. This is not
      * mandatory to do. This will produce a "complete" template for which no more constraints can be specified.
      *
      * @constructor
      *   creates a new instance of this class specifying the minimum value, being a [[scala.util.Left]] if it inclusive, or a
      *   [[scala.util.Right]] if it is exclusive
      */
  class WithMinimumFloatTemplate(min: Either[Float, Float])
    extends WithMinimumNumericTemplate[Float, CompleteFloatTemplate](
      min,
      new CompleteFloatTemplate(_, _),
      min => JsonFloatTemplate(None, min.left.toOption, None, min.toOption, None)
    )

    /** The float template for which the maximum value, either inclusive or exclusive, has been specified.
      *
      * This template represents one for float values where the maximum constraint, whether it be an inclusive or exclusive one,
      * has been specified. Now, only a minimum constraint can be specified, closing the now half-open interval. This is not
      * mandatory to do. This will produce a "complete" template for which no more constraints can be specified.
      *
      * @constructor
      *   creates a new instance of this class specifying the maximum value, being a [[scala.util.Left]] if it inclusive, or a
      *   [[scala.util.Right]] if it is exclusive
      */
  class WithMaximumFloatTemplate(max: Either[Float, Float])
    extends WithMaximumNumericTemplate[Float, CompleteFloatTemplate](
      max,
      new CompleteFloatTemplate(_, _),
      max => JsonFloatTemplate(None, None, max.left.toOption, None, max.toOption)
    )

    /** The "terminal" float template, for which no more information can be specified for building a [[JsonFloatTemplate]].
      *
      * This template represents the last stage in building a numeric template, meaning that no more constraints can be specified.
      * The range for the value has already been specified as a minimum and maximum allowed values, either inclusive or exclusive.
      *
      * @constructor
      *   creates a new instance of this class specifying the minimum and the maximum values, being [[scala.util.Left]]s if they
      *   are inclusive, or [[scala.util.Right]]s if they are exclusive
      */
  class CompleteFloatTemplate(
    min: Either[Float, Float],
    max: Either[Float, Float]
  ) extends CompleteNumericTemplate[Float](
      min,
      max,
      (min, max) => JsonFloatTemplate(None, min.left.toOption, max.left.toOption, min.toOption, max.toOption)
    )
}
