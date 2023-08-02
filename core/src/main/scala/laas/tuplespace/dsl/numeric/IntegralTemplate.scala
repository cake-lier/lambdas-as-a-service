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
import laas.tuplespace.dsl.Template

/** The trait for generalizing the creation of integral [[JsonTemplate]]s.
  *
  * This trait allows to create a family of traits which represents the states in which an integral template can be in after
  * specifying some constraint or other. The template starts "empty", meaning that it is able to match all numeric values for its
  * type. Then, a minimum or maximum value can be specified, either inclusive or exclusive. Another constraint allows to specify
  * the missing half part of the interval. The interval can be left half-open and its limits can be specified in any order. The
  * last constraint that can be specified is the "multiple of" one, which allows to constrain the values that the template can
  * match to only the integral values which are multiple of the given one. After this, template is "complete" and without further
  * constraints to apply. This last constraint can be inserted in any moment during its creation: before, during, or after the
  * specification of the range constraint.
  */
trait IntegralTemplate extends NumericTemplate {

  import scala.annotation.targetName

  /** The "empty" integral template, capable of matching any value of the given integral type.
    *
    * This is the starting template from which creating any other integral template by specifying more and more constraints. Being
    * so, it is allowed to add a minimum value, either inclusive or exclusive, or a maximum value, either inclusive or exclusive,
    * to create a new template from this. It is also allowed to specify a number of which the matched integral number should be
    * multiple of for creating a new template.
    *
    * @tparam A
    *   the actual integral type for which instantiating this type
    * @tparam B
    *   the actual type of the template for which the minimum has been specified
    * @tparam C
    *   the actual type of the template for which the maximum has been specified
    * @tparam D
    *   the actual type of the template for which either the minimum and the maximum have been specified altogether
    * @tparam E
    *   the actual type of the template for which either the minimum and the divisor have been specified altogether
    * @tparam F
    *   the actual type of the template for which either the divisor and the maximum have been specified altogether
    * @tparam G
    *   the actual type of the template for which the divisor has been specified
    * @tparam H
    *   the actual type of the template for which all possible constraints have been specified altogether
    * @constructor
    *   creates a new instance of this trait given the function that returns a new template given the minimum, the one that
    *   returns a new template given the maximum, the one that returns a new template given the divisor and the one that returns a
    *   new [[JsonIntegralTemplate]] from this one
    */
  trait EmptyIntegralTemplate[
    A: Integral,
    B <: WithMinimumIntegralTemplate[A, D, E, H],
    C <: WithMaximumIntegralTemplate[A, D, F, H],
    D <: WithMinWithMaxIntegralTemplate[A, H],
    E <: WithMinWithMultipleIntegralTemplate[A, H],
    F <: WithMaxWithMultipleIntegralTemplate[A, H],
    G <: WithMultipleIntegralTemplate[A, E, F, H],
    H <: CompleteIntegralTemplate[A]
  ](
    withMinBuilder: Either[A, A] => B,
    withMaxBuilder: Either[A, A] => C,
    withMultipleBuilder: A => G,
    converter: () => JsonIntegralTemplate[A]
  ) extends Template {

      /** Adds the inclusive minimum that the integral value should satisfy to match the built template.
        *
        * @param min
        *   the inclusive minimum that the integral value should satisfy to match the built template
        * @return
        *   a template which has stored the inclusive minimum information
        */
    def gte(min: A): B = withMinBuilder(Left[A, A](min))

    /** Adds the exclusive minimum that the integral value should satisfy to match the built template.
      *
      * @param min
      *   the exclusive minimum that the integral value should satisfy to match the built template
      * @return
      *   a template which has stored the exclusive minimum information
      */
    def gt(min: A): B = withMinBuilder(Right[A, A](min))

    /** Adds the inclusive maximum that the integral value should satisfy to match the built template.
      *
      * @param max
      *   the inclusive maximum that the integral value should satisfy to match the built template
      * @return
      *   a template which has stored the inclusive maximum information
      */
    def lte(max: A): C = withMaxBuilder(Left[A, A](max))

    /** Adds the exclusive maximum that the integral value should satisfy to match the built template.
      *
      * @param max
      *   the exclusive maximum that the integral value should satisfy to match the built template
      * @return
      *   a template which has stored the exclusive maximum information
      */
    def lt(max: A): C = withMaxBuilder(Right[A, A](max))

    /** Adds the number that the integral value should be multiple of to match the built template.
      *
      * @param multipleOf
      *   the number that the integral value should be multiple of to match the built template
      * @return
      *   a template which has stored the "multiple of" information
      */
    def div(multipleOf: A): G = withMultipleBuilder(multipleOf)

    override def toJsonTemplate: JsonTemplate = converter()
  }

  /** The integral template for which the "multiple of" value has been specified.
    *
    * This template represents one for integral values where the "multiple of" constraint has been specified. Now, a minimum or a
    * maximum constraint can be specified, creating a range of acceptable values for the template to match. Neither of this
    * constraints are mandatory to apply. This will produce a template for which either the minimum and the "multiple of" values
    * have been specified, or the maximum and the "multiple of" values have been specified.
    *
    * @tparam A
    *   the actual integral type for which instantiating this type
    * @tparam B
    *   the actual type of the template for which either the minimum and the divisor have been specified altogether
    * @tparam C
    *   the actual type of the template for which either the maximum and the divisor have been specified altogether
    * @tparam D
    *   the actual type of the template for which all possible constraints have been specified altogether
    * @constructor
    *   creates a new instance of this trait given the already specified "multiple of" value, the function that returns a new
    *   template given the minimum and the "multiple of" value, the function that returns a new template given the maximum and the
    *   "multiple of" value and the one that returns a new [[JsonIntegralTemplate]] from this one
    */
  trait WithMultipleIntegralTemplate[
    A: Integral,
    B <: WithMinWithMultipleIntegralTemplate[A, D],
    C <: WithMaxWithMultipleIntegralTemplate[A, D],
    D <: CompleteIntegralTemplate[A]
  ](
    multipleOf: A,
    withMinWithMultipleBuilder: (Either[A, A], A) => B,
    withMaxWithMultipleBuilder: (Either[A, A], A) => C,
    converter: Option[A] => JsonIntegralTemplate[A]
  ) extends Template {

      /** Adds the inclusive minimum that the integral value should satisfy to match the built template.
        *
        * @param min
        *   the inclusive minimum that the integral value should satisfy to match the built template
        * @return
        *   a template which has stored the inclusive minimum and the "multiple of" information
        */
    def gte(min: A): B = withMinWithMultipleBuilder(Left[A, A](min), multipleOf)

    /** Adds the exclusive minimum that the integral value should satisfy to match the built template.
      *
      * @param min
      *   the exclusive minimum that the integral value should satisfy to match the built template
      * @return
      *   a template which has stored the exclusive minimum and the "multiple of" information
      */
    def gt(min: A): B = withMinWithMultipleBuilder(Right[A, A](min), multipleOf)

    /** Adds the inclusive maximum that the integral value should satisfy to match the built template.
      *
      * @param max
      *   the inclusive maximum that the integral value should satisfy to match the built template
      * @return
      *   a template which has stored the inclusive maximum and the "multiple of" information
      */
    def lte(max: A): C = withMaxWithMultipleBuilder(Left[A, A](max), multipleOf)

    /** Adds the exclusive maximum that the integral value should satisfy to match the built template.
      *
      * @param max
      *   the exclusive maximum that the integral value should satisfy to match the built template
      * @return
      *   a template which has stored the exclusive maximum and the "multiple of" information
      */
    def lt(max: A): C = withMaxWithMultipleBuilder(Right[A, A](max), multipleOf)

    override def toJsonTemplate: JsonTemplate = converter(Some(multipleOf))
  }

  /** The integral template for which the minimum value, either inclusive or exclusive, has been specified.
    *
    * This template represents one for integral values where the minimum constraint, whether it be an inclusive or exclusive one,
    * has been specified. Now, a maximum constraint can be specified, closing the now half-open interval, or a "multiple of"
    * constraint can be specified, allowing to match only the values which are multiple of this number. Neither of this
    * constraints are mandatory to apply. This will produce a template for which either the minimum and the maximum values have
    * been specified, or the minimum and the "multiple of" values have been specified.
    *
    * @tparam A
    *   the actual integral type for which instantiating this type
    * @tparam B
    *   the actual type of the template for which either the minimum and the maximum have been specified altogether
    * @tparam C
    *   the actual type of the template for which either the minimum and the divisor have been specified altogether
    * @tparam D
    *   the actual type of the template for which all possible constraints have been specified altogether
    * @constructor
    *   creates a new instance of this trait given the already specified minimum, the function that returns a new template given
    *   the minimum and the maximum, the function that returns a new template given the minimum and the "multiple of" value and
    *   the one that returns a new [[JsonIntegralTemplate]] from this one
    */
  trait WithMinimumIntegralTemplate[
    A: Integral,
    B <: WithMinWithMaxIntegralTemplate[A, D],
    C <: WithMinWithMultipleIntegralTemplate[A, D],
    D <: CompleteIntegralTemplate[A]
  ](
    min: Either[A, A],
    withMinWithMaxBuilder: (Either[A, A], Either[A, A]) => B,
    withMinWithMultipleBuilder: (Either[A, A], A) => C,
    converter: Either[A, A] => JsonIntegralTemplate[A]
  ) extends Template {

      /** Adds the inclusive maximum that the integral value should satisfy to match the built template.
        *
        * @param max
        *   the inclusive maximum that the integral value should satisfy to match the built template
        * @return
        *   a template which has stored the minimum and the inclusive maximum information
        */
    def lte(max: A): B = withMinWithMaxBuilder(min, Left[A, A](max))

    /** Adds the exclusive maximum that the integral value should satisfy to match the built template.
      *
      * @param max
      *   the exclusive maximum that the integral value should satisfy to match the built template
      * @return
      *   a template which has stored the minimum and the exclusive maximum information
      */
    def lt(max: A): B = withMinWithMaxBuilder(min, Right[A, A](max))

    /** Adds the number that the integral value should be multiple of to match the built template.
      *
      * @param multipleOf
      *   the number that the integral value should be multiple of to match the built template
      * @return
      *   a template which has stored the minimum and the "multiple of" information
      */
    def div(multipleOf: A): C = withMinWithMultipleBuilder(min, multipleOf)

    override def toJsonTemplate: JsonTemplate = converter(min)
  }

  /** The integral template for which the maximum value, either inclusive or exclusive, has been specified.
    *
    * This template represents one for integral values where the maximum constraint, whether it be an inclusive or exclusive one,
    * has been specified. Now, a minimum constraint can be specified, closing the now half-open interval, or a "multiple of"
    * constraint can be specified, allowing to match only the values which are multiple of this number. Neither of this
    * constraints are mandatory to apply. This will produce a template for which either the minimum and the maximum values have
    * been specified, or the maximum and the "multiple of" values have been specified.
    *
    * @tparam A
    *   the actual integral type for which instantiating this type
    * @tparam B
    *   the actual type of the template for which either the minimum and the maximum have been specified altogether
    * @tparam C
    *   the actual type of the template for which either the maximum and the divisor have been specified altogether
    * @tparam D
    *   the actual type of the template for which all possible constraints have been specified altogether
    * @constructor
    *   creates a new instance of this trait given the already specified maximum, the function that returns a new template given
    *   the minimum and the maximum, the function that returns a new template given the maximum and the "multiple of" value and
    *   the one that returns a new [[JsonIntegralTemplate]] from this one
    */
  trait WithMaximumIntegralTemplate[
    A: Numeric,
    B <: WithMinWithMaxIntegralTemplate[A, D],
    C <: WithMaxWithMultipleIntegralTemplate[A, D],
    D <: CompleteIntegralTemplate[A]
  ](
    max: Either[A, A],
    withMinWithMaxBuilder: (Either[A, A], Either[A, A]) => B,
    withMaxWithMultipleBuilder: (Either[A, A], A) => C,
    converter: Either[A, A] => JsonIntegralTemplate[A]
  ) extends Template {

      /** Adds the inclusive minimum that the integral value should satisfy to match the built template.
        *
        * @param min
        *   the inclusive minimum that the integral value should satisfy to match the built template
        * @return
        *   a template which has stored the inclusive minimum and maximum information
        */
    def gte(min: A): B = withMinWithMaxBuilder(Left[A, A](min), max)

    /** Adds the exclusive minimum that the integral value should satisfy to match the built template.
      *
      * @param min
      *   the exclusive minimum that the integral value should satisfy to match the built template
      * @return
      *   a template which has stored the exclusive minimum and the maximum information
      */
    def gt(min: A): B = withMinWithMaxBuilder(Right[A, A](min), max)

    /** Adds the number that the integral value should be multiple of to match the built template.
      *
      * @param multipleOf
      *   the number that the integral value should be multiple of to match the built template
      * @return
      *   a template which has stored the "multiple of" and the maximum information
      */
    def div(multipleOf: A): C = withMaxWithMultipleBuilder(max, multipleOf)

    override def toJsonTemplate: JsonTemplate = converter(max)
  }

  /** The integral template for which the minimum and the maximum values, either inclusive or exclusive, have been specified.
    *
    * This template represents one for integral values where the minimum and the maximum constraints, whether they are inclusive
    * or exclusive, have been specified. Now, a "multiple of" constraint can be specified, allowing to match only the values which
    * are multiple of this number. This constraints is not mandatory to apply. This will produce a "complete" template for which
    * no more constraints can be specified.
    *
    * @tparam A
    *   the actual integral type for which instantiating this type
    * @tparam B
    *   the actual type of the template for which all possible constraints have been specified altogether
    * @constructor
    *   creates a new instance of this trait given the already specified minimum and maximum values, the function that returns a
    *   new template given the minimum, the maximum and the "multiple of" value and the one that returns a new
    *   [[JsonIntegralTemplate]] from this one
    */
  trait WithMinWithMaxIntegralTemplate[A: Numeric, B <: CompleteIntegralTemplate[A]](
    min: Either[A, A],
    max: Either[A, A],
    completeBuilder: (Either[A, A], Either[A, A], A) => B,
    converter: (Either[A, A], Either[A, A]) => JsonNumericTemplate[A]
  ) extends Template {

      /** Adds the number that the integral value should be multiple of to match the built template.
        *
        * @param multipleOf
        *   the number that the integral value should be multiple of to match the built template
        * @return
        *   a "terminal" template, which has stored the minimum, the maximum and the "multiple of" information
        */
    def div(multipleOf: A): B = completeBuilder(min, max, multipleOf)

    override def toJsonTemplate: JsonTemplate = converter(min, max)
  }

  /** The integral template for which the minimum value, either inclusive or exclusive, has been specified along with the
    * "multipleOf" value.
    *
    * This template represents one for integral values where the minimum constraint, whether it be an inclusive or exclusive one,
    * have been specified alongside a "multiple of" constraint. Now, a maximum constraint can be specified, allowing to close the
    * half-open interval previously created. This constraints is not mandatory to apply. This will produce a "complete" template
    * for which no more constraints can be specified.
    *
    * @tparam A
    *   the actual integral type for which instantiating this type
    * @tparam B
    *   the actual type of the template for which all possible constraints have been specified altogether
    * @constructor
    *   creates a new instance of this trait given the already specified minimum and "multiple of" values, the function that
    *   returns a new template given the minimum, the maximum and the "multiple of" value and the one that returns a new
    *   [[JsonIntegralTemplate]] from this one
    */
  trait WithMinWithMultipleIntegralTemplate[A: Integral, B <: CompleteIntegralTemplate[A]](
    min: Either[A, A],
    multipleOf: A,
    completeBuilder: (Either[A, A], Either[A, A], A) => B,
    converter: (Either[A, A], Option[A]) => JsonIntegralTemplate[A]
  ) extends Template {

      /** Adds the inclusive maximum that the integral value should satisfy to match the built template.
        *
        * @param max
        *   the inclusive maximum that the integral value should satisfy to match the built template
        * @return
        *   a "terminal" template, which has stored the minimum, the maximum and the "multiple of" information
        */
    def lte(max: A): B = completeBuilder(min, Left[A, A](max), multipleOf)

    /** Adds the exclusive maximum that the integral value should satisfy to match the built template.
      *
      * @param max
      *   the exclusive maximum that the integral value should satisfy to match the built template
      * @return
      *   a "terminal" template, which has stored the minimum, the maximum and the "multiple of" information
      */
    def lt(max: A): B = completeBuilder(min, Right[A, A](max), multipleOf)

    override def toJsonTemplate: JsonTemplate = converter(min, Some(multipleOf))
  }

  /** The integral template for which the maximum value, either inclusive or exclusive, has been specified along with the
    * "multipleOf" value.
    *
    * This template represents one for integral values where the maximum constraint, whether it be an inclusive or exclusive one,
    * have been specified alongside a "multiple of" constraint. Now, a minimum constraint can be specified, allowing to close the
    * half-open interval previously created. This constraints is not mandatory to apply. This will produce a "complete" template
    * for which no more constraints can be specified.
    *
    * @tparam A
    *   the actual integral type for which instantiating this type
    * @tparam B
    *   the actual type of the template for which all possible constraints have been specified altogether
    * @constructor
    *   creates a new instance of this trait given the already specified "multiple of" and maximum values, the function that
    *   returns a new template given the minimum, the maximum and the "multiple of" value and the one that returns a new
    *   [[JsonIntegralTemplate]] from this one
    */
  trait WithMaxWithMultipleIntegralTemplate[A: Integral, B <: CompleteIntegralTemplate[A]](
    max: Either[A, A],
    multipleOf: A,
    completeBuilder: (Either[A, A], Either[A, A], A) => B,
    converter: (Either[A, A], Option[A]) => JsonIntegralTemplate[A]
  ) extends Template {

      /** Adds the inclusive minimum that the integral value should satisfy to match the built template.
        *
        * @param min
        *   the inclusive minimum that the integral value should satisfy to match the built template
        * @return
        *   a "terminal" template, which has stored the minimum, the maximum and the "multiple of" information
        */
    def gte(min: A): B = completeBuilder(Left[A, A](min), max, multipleOf)

    /** Adds the exclusive minimum that the integral value should satisfy to match the built template.
      *
      * @param min
      *   the exclusive minimum that the integral value should satisfy to match the built template
      * @return
      *   a "terminal" template, which has stored the minimum, the maximum and the "multiple of" information
      */
    def gt(min: A): B = completeBuilder(Right[A, A](min), max, multipleOf)

    override def toJsonTemplate: JsonTemplate = converter(max, Some(multipleOf))
  }

  /** The "terminal" integral template, for which no more information can be specified for building a [[JsonIntegralTemplate]].
    *
    * This template represents the last stage in building an integral template, meaning that no more constraints can be specified.
    * The range for the value has already been specified as a minimum and maximum allowed values, either inclusive or exclusive,
    * so as the number which it should be a multiple of.
    *
    * @tparam A
    *   the actual integral type for which instantiating this type
    * @constructor
    *   creates a new instance of this trait given the already specified minimum, maximum and "multiple of" values and the
    *   function that returns a new [[JsonIntegralTemplate]] from this one
    */
  trait CompleteIntegralTemplate[A: Integral](
    min: Either[A, A],
    max: Either[A, A],
    multipleOf: A,
    converter: (Either[A, A], Either[A, A], Option[A]) => JsonIntegralTemplate[A]
  ) extends Template {

    override def toJsonTemplate: JsonTemplate = converter(min, max, Some(multipleOf))
  }
}
