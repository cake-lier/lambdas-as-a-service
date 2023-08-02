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

/** The object implementing the [[IntegralTemplate]] trait for the long type.
  *
  * This object represents the specific implementation of the family of traits defined by the [[IntegralTemplate]] one. It then
  * implements them all specifying that the generic numeric type that is specified in all of them is the [[Int]] one.
  */
object LongTemplate extends IntegralTemplate {

    /** The "empty" long template, capable of matching any value of the long type.
      *
      * This is the starting template from which creating any other long template by specifying more and more constraints. Being
      * so, it is allowed to add a minimum value, either inclusive or exclusive, or a maximum value, either inclusive or
      * exclusive, to create a new template from this. It is also allowed to specify a number of which the matched long should be
      * multiple of for creating a new template.
      */
  class EmptyLongTemplate
    extends EmptyIntegralTemplate[
      Long,
      WithMinimumLongTemplate,
      WithMaximumLongTemplate,
      WithMinWithMaxLongTemplate,
      WithMinWithMultipleLongTemplate,
      WithMaxWithMultipleLongTemplate,
      WithMultipleLongTemplate,
      CompleteLongTemplate
    ](
      new WithMinimumLongTemplate(_),
      new WithMaximumLongTemplate(_),
      new WithMultipleLongTemplate(_),
      () => JsonLongTemplate(None, None, None, None, None, None)
    )

    /** The long template for which the "multiple of" value has been specified.
      *
      * This template represents one for long values where the "multiple of" constraint has been specified. Now, a minimum or a
      * maximum constraint can be specified, creating a range of acceptable values for the template to match. Neither of this
      * constraints are mandatory to apply. This will produce a template for which either the minimum and the "multiple of" values
      * have been specified, or the maximum and the "multiple of" values have been specified.
      *
      * @constructor
      *   creates a new instance of this class specifying the divisor value
      */
  class WithMultipleLongTemplate(multipleOf: Long)
    extends WithMultipleIntegralTemplate[
      Long,
      WithMinWithMultipleLongTemplate,
      WithMaxWithMultipleLongTemplate,
      CompleteLongTemplate
    ](
      multipleOf,
      new WithMinWithMultipleLongTemplate(_, _),
      new WithMaxWithMultipleLongTemplate(_, _),
      JsonLongTemplate(None, _, None, None, None, None)
    )

    /** The long template for which the minimum value, either inclusive or exclusive, has been specified.
      *
      * This template represents one for long values where the minimum constraint, whether it be an inclusive or exclusive one,
      * has been specified. Now, a maximum constraint can be specified, closing the now half-open interval, or a "multiple of"
      * constraint can be specified, allowing to match only the values which are multiple of this number. Neither of this
      * constraints are mandatory to apply. This will produce a template for which either the minimum and the maximum values have
      * been specified, or the minimum and the "multiple of" values have been specified.
      *
      * @constructor
      *   creates a new instance of this class specifying the minimum value, being a [[scala.util.Left]] if it inclusive, or a
      *   [[scala.util.Right]] if it is exclusive
      */
  class WithMinimumLongTemplate(min: Either[Long, Long])
    extends WithMinimumIntegralTemplate[
      Long,
      WithMinWithMaxLongTemplate,
      WithMinWithMultipleLongTemplate,
      CompleteLongTemplate
    ](
      min,
      new WithMinWithMaxLongTemplate(_, _),
      new WithMinWithMultipleLongTemplate(_, _),
      min => JsonLongTemplate(None, None, min.left.toOption, None, min.toOption, None)
    )

    /** The long template for which the maximum value, either inclusive or exclusive, has been specified.
      *
      * This template represents one for long values where the maximum constraint, whether it be an inclusive or exclusive one,
      * has been specified. Now, a minimum constraint can be specified, closing the now half-open interval, or a "multiple of"
      * constraint can be specified, allowing to match only the values which are multiple of this number. Neither of this
      * constraints are mandatory to apply. This will produce a template for which either the minimum and the maximum values have
      * been specified, or the maximum and the "multiple of" values have been specified.
      *
      * @constructor
      *   creates a new instance of this class specifying the maximum value, being a [[scala.util.Left]] if it inclusive, or a
      *   [[scala.util.Right]] if it is exclusive
      */
  class WithMaximumLongTemplate(max: Either[Long, Long])
    extends WithMaximumIntegralTemplate[
      Long,
      WithMinWithMaxLongTemplate,
      WithMaxWithMultipleLongTemplate,
      CompleteLongTemplate
    ](
      max,
      new WithMinWithMaxLongTemplate(_, _),
      new WithMaxWithMultipleLongTemplate(_, _),
      max => JsonLongTemplate(None, None, None, max.left.toOption, None, max.toOption)
    )

    /** The long template for which the minimum and the maximum values, either inclusive or exclusive, have been specified.
      *
      * This template represents one for long values where the minimum and the maximum constraints, whether they are inclusive or
      * exclusive, have been specified. Now, a "multiple of" constraint can be specified, allowing to match only the values which
      * are multiple of this number. This constraints is not mandatory to apply. This will produce a "complete" template for which
      * no more constraints can be specified.
      *
      * @constructor
      *   creates a new instance of this class specifying the minimum and the maximum values, being [[scala.util.Left]]s if they
      *   are inclusive, or [[scala.util.Right]]s if they are exclusive
      */
  class WithMinWithMaxLongTemplate(min: Either[Long, Long], max: Either[Long, Long])
    extends WithMinWithMaxIntegralTemplate[
      Long,
      CompleteLongTemplate
    ](
      min,
      max,
      new CompleteLongTemplate(_, _, _),
      (min, max) => JsonLongTemplate(None, None, min.left.toOption, max.left.toOption, min.toOption, max.toOption)
    )

    /** The long template for which the minimum value, either inclusive or exclusive, has been specified along with the
      * "multipleOf" value.
      *
      * This template represents one for long values where the minimum constraint, whether it be an inclusive or exclusive one,
      * have been specified alongside a "multiple of" constraint. Now, a maximum constraint can be specified, allowing to close
      * the half-open interval previously created. This constraints is not mandatory to apply. This will produce a "complete"
      * template for which no more constraints can be specified.
      *
      * @constructor
      *   creates a new instance of this class specifying the minimum value, being a [[scala.util.Left]] if it inclusive, or a
      *   [[scala.util.Right]] if it is exclusive, and the "multiple of" value
      */
  class WithMinWithMultipleLongTemplate(min: Either[Long, Long], multipleOf: Long)
    extends WithMinWithMultipleIntegralTemplate[
      Long,
      CompleteLongTemplate
    ](
      min,
      multipleOf,
      new CompleteLongTemplate(_, _, _),
      (min, multipleOf) => JsonLongTemplate(None, multipleOf, min.left.toOption, None, min.toOption, None)
    )

    /** The integral template for which the maximum value, either inclusive or exclusive, has been specified along with the
      * "multipleOf" value.
      *
      * This template represents one for integral values where the maximum constraint, whether it be an inclusive or exclusive
      * one, have been specified alongside a "multiple of" constraint. Now, a minimum constraint can be specified, allowing to
      * close the half-open interval previously created. This constraints is not mandatory to apply. This will produce a
      * "complete" template for which no more constraints can be specified.
      *
      * @constructor
      *   creates a new instance of this class specifying the maximum value, being a [[scala.util.Left]] if it inclusive, or a
      *   [[scala.util.Right]] if it is exclusive, and the "multiple of" value
      */
  class WithMaxWithMultipleLongTemplate(max: Either[Long, Long], multipleOf: Long)
    extends WithMaxWithMultipleIntegralTemplate[
      Long,
      CompleteLongTemplate
    ](
      max,
      multipleOf,
      new CompleteLongTemplate(_, _, _),
      (max, multipleOf) => JsonLongTemplate(None, multipleOf, None, max.left.toOption, None, max.toOption)
    )

    /** The "terminal" long template, for which no more information can be specified for building a [[JsonIntTemplate]].
      *
      * This template represents the last stage in building an long template, meaning that no more constraints can be specified.
      * The range for the value has already been specified as a minimum and maximum allowed values, either inclusive or exclusive,
      * so as the number which it should be a multiple of.
      *
      * @constructor
      *   creates a new instance of this class specifying the minimum and the maximum values, being [[scala.util.Left]]s if they
      *   are inclusive, or [[scala.util.Right]]s if they are exclusive, and the "multiple of" value
      */
  class CompleteLongTemplate(min: Either[Long, Long], max: Either[Long, Long], multipleOf: Long)
    extends CompleteIntegralTemplate[Long](
      min,
      max,
      multipleOf,
      (min, max, multipleOf) =>
        JsonLongTemplate(None, multipleOf, min.left.toOption, max.left.toOption, min.toOption, max.toOption)
    )
}
