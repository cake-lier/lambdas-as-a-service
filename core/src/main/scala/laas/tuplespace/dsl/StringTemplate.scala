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
package laas.tuplespace.dsl

import laas.tuplespace.JsonTemplate
import JsonTemplate.JsonStringTemplate

/** The object collecting all DSL templates for creating new instances of [[JsonStringTemplate]]s.
  *
  * Any new "string" template starts as "empty", meaning that only the type of the element will be checked for matching and the
  * template will be capable of matching any string. Then, for further specifying the template, the user can decide whether to add
  * some values to which the string should be equal, or a regular expression which should match the string, or an inclusive range
  * of lengths for the string to match. The interval can be half-open on both ends and those can be specified in either order.
  */
object StringTemplate {

  import scala.util.matching.Regex

  /** The base template, capable of matching any string value. */
  class EmptyStringTemplate extends Template {

      /** Adds the values to which the string should be equal in order to match the built template.
        *
        * @param values
        *   the values to which the string should be equal for it to match the built template
        * @return
        *   a "terminal" template, which has stored this information
        */
    def in(values: String*): CompleteStringTemplate = CompleteStringTemplate(Some(values.toSet), None, None, None)

    /** Adds the regular expression which should match the string for this to match the built template.
      *
      * @param regex
      *   the regex which should match the string for it to match the built template
      * @return
      *   a "terminal" template, which has stored this information
      */
    def matches(regex: Regex): CompleteStringTemplate = CompleteStringTemplate(None, None, None, Some(regex))

    /** Adds the inclusive minimum length that the string should satisfy to match the built template.
      *
      * @param minLength
      *   the inclusive minimum length that the string should satisfy to match the built template
      * @return
      *   a template which has stored the inclusive minimum information
      */
    def gte(minLength: Int): WithMinimumStringTemplate = WithMinimumStringTemplate(minLength)

    /** Adds the inclusive maximum length that the string should satisfy to match the built template.
      *
      * @param maxLength
      *   the inclusive maximum length that the string should satisfy to match the built template
      * @return
      *   a template which has stored the inclusive maximum information
      */
    def lte(maxLength: Int): WithMaximumStringTemplate = WithMaximumStringTemplate(maxLength)

    override def toJsonTemplate: JsonTemplate = JsonStringTemplate(None, None, None, None)
  }

  /** The string template for which only the inclusive minimum length has been specified. */
  class WithMinimumStringTemplate(minLength: Int) extends Template {

      /** Adds the inclusive maximum length that the string should satisfy to match the built template.
        *
        * @param maxLength
        *   the inclusive maximum length that the string should satisfy to match the built template
        * @return
        *   a "terminal" template, which has stored the inclusive minimum and inclusive maximum information
        */
    def lte(maxLength: Int): CompleteStringTemplate = CompleteStringTemplate(None, Some(minLength), Some(maxLength), None)

    override def toJsonTemplate: JsonTemplate = JsonStringTemplate(None, Some(minLength), None, None)
  }

  /** The string template for which only the inclusive maximum length has been specified. */
  class WithMaximumStringTemplate(maxLength: Int) extends Template {

      /** Adds the inclusive minimum length that the string should satisfy to match the built template.
        *
        * @param minLength
        *   the inclusive minimum length that the string should satisfy to match the built template
        * @return
        *   a "terminal" template, which has stored the inclusive minimum and inclusive maximum information
        */
    def gte(minLength: Int): CompleteStringTemplate = CompleteStringTemplate(None, Some(minLength), Some(maxLength), None)

    override def toJsonTemplate: JsonTemplate = JsonStringTemplate(None, None, Some(maxLength), None)
  }

  /** The "terminal" string template, for which no more information can be specified for building a [[JsonStringTemplate]]. */
  class CompleteStringTemplate(
    values: Option[Set[String]],
    minLength: Option[Int],
    maxLength: Option[Int],
    pattern: Option[Regex]
  ) extends Template {

    override def toJsonTemplate: JsonTemplate = JsonStringTemplate(values, minLength, maxLength, pattern)
  }
}
