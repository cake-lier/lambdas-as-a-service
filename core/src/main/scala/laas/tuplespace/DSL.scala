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

import scala.annotation.targetName

import laas.tuplespace.dsl.*
import laas.tuplespace.dsl.StringTemplate.*
import laas.tuplespace.dsl.numeric.DoubleTemplate.*
import laas.tuplespace.dsl.numeric.FloatTemplate.*
import laas.tuplespace.dsl.numeric.IntTemplate.*
import laas.tuplespace.dsl.numeric.LongTemplate.*

/** The object allowing the access to the DSL for creating [[JsonTupleTemplate]]s.
  *
  * This objects contains all methods for creating a tuple template with simplicity and conciseness. The starting point are the
  * [[complete]] and [[partial]] methods, to be used for allowing the creation of new [[JsonTupleTemplate]]s. These are the only
  * top-level templates allowed to be created. All other templates can be used as elements of this type of templates. An example
  * of syntax using this DSL is:
  *
  * <code>complete(string in ("event-type-1", "event-type-2"), *, anyOf(bool, nil))</code>
  *
  * If a constant value is to be used for matching, it can be inserted as any other template in a tuple template, for example:
  *
  * <code>partial("event-type-1", 5.33f, *)</code>
  *
  * The use of this DSL is the main tool for creating new [[JsonTemplate]]s of any kind, because it is able to limit the ways in
  * which a template can be created. The API exposed by the [[JsonTemplate]] trait and all its subtypes it is not designed to
  * capture the constraints of their creation or the use cases for this library, while this DSL is. Being so, it is highly
  * encouraged to use DSL for creating templates.
  */
object DSL {

  private type ConstantElement = Int | Long | Float | Double | Boolean | String | JsonTuple

  /* Converts a constant element into a DSL template or, if the element is already a template, it does nothing. */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion", "org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
  private def convertToTemplate(v: Template | ConstantElement): Template = v match {
    case e: ConstantElement =>
      e match {
        case i: Int =>
          new Template {

            override def toJsonTemplate: JsonTemplate = JsonIntTemplate(Some(i), None, None, None, None, None)
          }
        case l: Long =>
          new Template {

            override def toJsonTemplate: JsonTemplate = JsonLongTemplate(Some(l), None, None, None, None, None)
          }
        case f: Float =>
          new Template {

            override def toJsonTemplate: JsonTemplate = JsonFloatTemplate(Some(f), None, None, None, None)
          }
        case d: Double =>
          new Template {

            override def toJsonTemplate: JsonTemplate = JsonDoubleTemplate(Some(d), None, None, None, None)
          }
        case b: Boolean =>
          new Template {

            override def toJsonTemplate: JsonTemplate = JsonBooleanTemplate(Some(b))
          }
        case s: String =>
          new Template {

            override def toJsonTemplate: JsonTemplate = JsonStringTemplate(Some(Set(s)), None, None, None)
          }
        case t: JsonTuple =>
          new Template {

            override def toJsonTemplate: JsonTemplate =
              JsonTupleTemplate(t.toSeq.map(convertToTemplate).map(_.toJsonTemplate), additionalItems = false)
          }
      }
    case t: Template => t
    case null =>
      new Template {

        override def toJsonTemplate: JsonTemplate = JsonNullTemplate
      }
  }

  /** Entrypoint method for this DSL, allows for creating a new [[JsonTupleTemplate]] without allowing for additional items.
    *
    * This methods is one of the two to be used for creating a new [[JsonTuple]] template, the difference between them being that
    * this one considers all [[JsonTemplate]]s specified in it to be matched in the exact number with the [[JsonElement]]s in the
    * given [[JsonTuple]]. A tuple with an arity greater or smaller than the number of elements in the created template will
    * automatically fail the match, but not with an equal arity. The syntax for creating a new template is simple: calling this
    * method while passing all the templates, in order, that must be matched with the corresponding elements in a given
    * [[JsonTuple]]. Every template must be created using this DSL.
    *
    * @param templates
    *   the templates that constitutes the elements of the returned [[JsonTupleTemplate]]
    * @return
    *   a new [[JsonTupleTemplate]] as specified
    */
  def complete(templates: (Template | ConstantElement)*): JsonTupleTemplate =
    JsonTupleTemplate(templates.map(convertToTemplate).map(_.toJsonTemplate), additionalItems = false)

    /** Entrypoint method for this DSL, allows for creating a new [[JsonTupleTemplate]] allowing for additional items.
      *
      * This methods is one of the two to be used for creating a new [[JsonTuple]] template, the difference between them being
      * that this one considers all [[JsonTemplate]]s specified in it to be matched by a number of [[JsonElement]]s in the given
      * [[JsonTuple]] which is at least equal to the number of the elements. A tuple with an arity smaller that the number of
      * elements in the created template will automatically fail the match, but not with an arity equal or greater. The syntax for
      * creating a new template is simple: calling this method while passing all the templates, in order, that must be matched
      * with the corresponding elements in a given [[JsonTuple]]. Every template must be created using this DSL.
      *
      * @param templates
      *   the templates that constitutes the elements of the returned [[JsonTupleTemplate]]
      * @return
      *   a new [[JsonTupleTemplate]] as specified
      */
  def partial(templates: (Template | ConstantElement)*): JsonTupleTemplate =
    JsonTupleTemplate(templates.map(convertToTemplate).map(_.toJsonTemplate), additionalItems = true)

    /** Returns a new "any" template, capable of matching any [[JsonElement]]. */
  @targetName("any")
  def `*` : Template = new Template {

    override def toJsonTemplate: JsonTemplate = JsonAnyTemplate
  }

  /** Returns a new "null" template, capable of matching only the <code>null</code> value. */
  def nil: Template = new Template {

    override def toJsonTemplate: JsonTemplate = JsonNullTemplate
  }

  /** Returns a new "float" template, capable of matching any [[Float]] value. */
  def float: EmptyFloatTemplate = EmptyFloatTemplate()

  /** Returns a new "double" template, capable of matching any [[Double]] value. */
  def double: EmptyDoubleTemplate = EmptyDoubleTemplate()

  /** Returns a new "boolean" template, capable of matching any [[Boolean]] value. */
  def bool: Template = new Template {

    override def toJsonTemplate: JsonTemplate = JsonBooleanTemplate(None)
  }

  /** Returns a new "string" template, capable of matching any [[String]] value. */
  def string: EmptyStringTemplate = EmptyStringTemplate()

  /** Returns a new "integer" template, capable of matching any [[Int]] value. */
  def int: EmptyIntTemplate = EmptyIntTemplate()

  /** Returns a new "long" template, capable of matching any [[Long]] value. */
  def long: EmptyLongTemplate = EmptyLongTemplate()

  /** Returns a new "allOf" template, a template that matches a [[JsonElement]] if and only if all templates which it contains
    * match.
    *
    * This method returns a special meta-template, which is built on top of other templates. For it to be matching a
    * [[JsonElement]], all templates that it contains must match the given [[JsonElement]].
    *
    * @param templates
    *   the templates to be used by this template
    * @return
    *   a template that matches a [[JsonElement]] if and only if all templates which it contains match
    */
  def allOf(templates: (Template | ConstantElement)*): Template = new Template {

    override def toJsonTemplate: JsonTemplate = JsonAllOfTemplate(templates.map(convertToTemplate).map(_.toJsonTemplate))
  }

  /** Returns a new "anyOf" template, a template that matches a [[JsonElement]] if and only if any template which it contains
    * match.
    *
    * This method returns a special meta-template, which is built on top of other templates. For it to be matching a
    * [[JsonElement]], any template that it contains must match the given [[JsonElement]].
    *
    * @param templates
    *   the templates to be used by this template
    * @return
    *   a template that matches a [[JsonElement]] if and only if any template which it contains match
    */
  def anyOf(templates: (Template | ConstantElement)*): Template = new Template {

    override def toJsonTemplate: JsonTemplate = JsonAnyOfTemplate(templates.map(convertToTemplate).map(_.toJsonTemplate))
  }

  /** Returns a new "oneOf" template, a template that matches a [[JsonElement]] if and only if exactly one template which it
    * contains match.
    *
    * This method returns a special meta-template, which is built on top of other templates. For it to be matching a
    * [[JsonElement]], exactly one template that it contains must match the given [[JsonElement]].
    *
    * @param templates
    *   the templates to be used by this template
    * @return
    *   a template that matches a [[JsonElement]] if and only if exactly one template which it contains match
    */
  def oneOf(templates: (Template | ConstantElement)*): Template = new Template {

    override def toJsonTemplate: JsonTemplate = JsonOneOfTemplate(templates.map(convertToTemplate).map(_.toJsonTemplate))
  }

  /** Returns a new "not" template, a template that matches a [[JsonElement]] if and only if the template which it contains does
    * not match.
    *
    * This method returns a special meta-template, which is built on top of another template. For it to be matching a
    * [[JsonElement]], the template that it contains must not match the given [[JsonElement]].
    *
    * @param template
    *   the template to be used by this template
    * @return
    *   a template that matches a [[JsonElement]] if and only if the template which it contains does not match
    */
  def not(template: Template | ConstantElement): Template = new Template {

    override def toJsonTemplate: JsonTemplate = JsonNotTemplate(convertToTemplate(template).toJsonTemplate)
  }
}

export DSL.*
