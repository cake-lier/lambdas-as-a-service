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

import laas.tuplespace.JsonTemplate
import laas.tuplespace.JsonTemplate.JsonNumericTemplate
import laas.tuplespace.dsl.Template

/** The trait for generalizing the creation of numeric [[JsonTemplate]]s.
  *
  * This trait allows to create a family of traits which represents the states in which a numeric template can be in after
  * specifying some constraint or other. The template starts "empty", meaning that it is able to match all numeric values for its
  * type. Then, a minimum or maximum value can be specified, either inclusive or exclusive. The last constraint allows to specify
  * the missing half part of the interval, leaving the template as "complete" and without further constraints to specify. The
  * interval can be left half-open and its limits can be specified in any order.
  */
trait NumericTemplate {

  /** The "empty" numeric template, capable of matching any value of the given numeric type.
    *
    * This is the starting template from which creating any other numeric template by specifying more and more constraints. Being
    * so, it is allowed to add a minimum value, either inclusive or exclusive, or a maximum value, either inclusive or exclusive,
    * to create a new template from this.
    *
    * @tparam A
    *   the actual numeric type for which instantiating this type
    * @tparam B
    *   the actual type of the template for which the minimum has been specified
    * @tparam C
    *   the actual type of the template for which the maximum has been specified
    * @tparam D
    *   the actual type of the template for which either the minimum and the maximum have been specified altogether
    * @constructor
    *   creates a new instance of this trait given the function that returns a new template given the minimum, the one that
    *   returns a new template given the maximum and the one that returns a new [[JsonNumericTemplate]] from this one
    */
  trait EmptyNumericTemplate[
    A: Numeric,
    B <: WithMinimumNumericTemplate[A, D],
    C <: WithMaximumNumericTemplate[A, D],
    D <: CompleteNumericTemplate[A]
  ](
    withMinBuilder: Either[A, A] => B,
    withMaxBuilder: Either[A, A] => C,
    converter: () => JsonNumericTemplate[A]
  ) extends Template {

      /** Adds the inclusive minimum that the numeric value should satisfy to match the built template.
        *
        * @param min
        *   the inclusive minimum that the numeric value should satisfy to match the built template
        * @return
        *   a template which has stored the inclusive minimum information
        */
    def gte(min: A): B = withMinBuilder(Left[A, A](min))

    /** Adds the exclusive minimum that the numeric value should satisfy to match the built template.
      *
      * @param min
      *   the exclusive minimum that the numeric value should satisfy to match the built template
      * @return
      *   a template which has stored the exclusive minimum information
      */
    def gt(min: A): B = withMinBuilder(Right[A, A](min))

    /** Adds the inclusive maximum that the numeric value should satisfy to match the built template.
      *
      * @param max
      *   the inclusive maximum that the numeric value should satisfy to match the built template
      * @return
      *   a template which has stored the inclusive maximum information
      */
    def lte(max: A): C = withMaxBuilder(Left[A, A](max))

    /** Adds the exclusive maximum that the numeric value should satisfy to match the built template.
      *
      * @param max
      *   the exclusive maximum that the numeric value should satisfy to match the built template
      * @return
      *   a template which has stored the exclusive maximum information
      */
    def lt(max: A): C = withMaxBuilder(Right[A, A](max))

    override def toJsonTemplate: JsonTemplate = converter()
  }

  /** The numeric template for which the minimum value, either inclusive or exclusive, has been specified.
    *
    * This template represents one for numerical values where the minimum constraint, whether it be an inclusive or exclusive one,
    * has been specified. Now, only a maximum constraint can be specified, closing the now half-open interval. This is not
    * mandatory to do. This will produce a "complete" template for which no more constraints can be specified.
    *
    * @tparam A
    *   the actual numeric type for which instantiating this type
    * @tparam B
    *   the actual type of the template for which either the minimum and the maximum have been specified altogether
    * @constructor
    *   creates a new instance of this trait given the already specified minimum value, the function that returns a new template
    *   given the minimum and the maximum and the one that returns a new [[JsonNumericTemplate]] from this one
    */
  trait WithMinimumNumericTemplate[A: Numeric, B <: CompleteNumericTemplate[A]](
    min: Either[A, A],
    completeBuilder: (Either[A, A], Either[A, A]) => B,
    converter: Either[A, A] => JsonNumericTemplate[A]
  ) extends Template {

      /** Adds the inclusive maximum that the numeric value should satisfy to match the built template.
        *
        * @param max
        *   the inclusive maximum that the numeric value should satisfy to match the built template
        * @return
        *   a "terminal" template, which has stored the minimum and the inclusive maximum information
        */
    def lte(max: A): B = completeBuilder(min, Left[A, A](max))

    /** Adds the exclusive maximum that the numeric value should satisfy to match the built template.
      *
      * @param max
      *   the exclusive maximum that the numeric value should satisfy to match the built template
      * @return
      *   a "terminal" template, which has stored the minimum and the exclusive maximum information
      */
    def lt(max: A): B = completeBuilder(min, Right[A, A](max))

    override def toJsonTemplate: JsonTemplate = converter(min)
  }

  /** The numeric template for which the maximum value, either inclusive or exclusive, has been specified.
    *
    * This template represents one for numerical values where the maximum constraint, whether it be an inclusive or exclusive one,
    * has been specified. Now, only a minimum constraint can be specified, closing the now half-open interval. This is not
    * mandatory to do. This will produce a "complete" template for which no more constraints can be specified.
    *
    * @tparam A
    *   the actual numeric type for which instantiating this type
    * @tparam B
    *   the actual type of the template for which either the minimum and the maximum have been specified altogether
    * @constructor
    *   creates a new instance of this trait given the already specified maximum value, the function that returns a new template
    *   given the minimum and the maximum and the one that returns a new [[JsonNumericTemplate]] from this one
    */
  trait WithMaximumNumericTemplate[A: Numeric, B <: CompleteNumericTemplate[A]](
    max: Either[A, A],
    completeBuilder: (Either[A, A], Either[A, A]) => B,
    converter: Either[A, A] => JsonNumericTemplate[A]
  ) extends Template {

      /** Adds the inclusive minimum that the numeric value should satisfy to match the built template.
        *
        * @param min
        *   the inclusive minimum that the numeric value should satisfy to match the built template
        * @return
        *   a "terminal" template, which has stored the inclusive minimum and the maximum information
        */
    def gte(min: A): B = completeBuilder(Left[A, A](min), max)

    /** Adds the exclusive minimum that the numeric value should satisfy to match the built template.
      *
      * @param min
      *   the exclusive minimum that the numeric value should satisfy to match the built template
      * @return
      *   a "terminal" template, which has stored the exclusive minimum and the maximum information
      */
    def gt(min: A): B = completeBuilder(Right[A, A](min), max)

    override def toJsonTemplate: JsonTemplate = converter(max)
  }

  /** The "terminal" numeric template, for which no more information can be specified for building a [[JsonNumericTemplate]].
    *
    * This template represents the last stage in building a numeric template, meaning that no more constraints can be specified.
    * The range for the value has already been specified as a minimum and maximum allowed values, either inclusive or exclusive.
    *
    * @tparam A
    *   the actual numeric type for which instantiating this type
    * @constructor
    *   creates a new instance of this trait given the already specified minimum and maximum values and the function that returns
    *   a new [[JsonNumericTemplate]] from this one
    */
  trait CompleteNumericTemplate[A: Numeric](
    min: Either[A, A],
    max: Either[A, A],
    converter: (Either[A, A], Either[A, A]) => JsonNumericTemplate[A]
  ) extends Template {

    override def toJsonTemplate: JsonTemplate = converter(min, max)
  }
}
