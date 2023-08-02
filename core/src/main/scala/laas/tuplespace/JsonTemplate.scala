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

import java.util.Objects

import AnyOps.*

/** A template, also known as an anti-tuple, an entity capable of matching against a tuple.
  *
  * Being the tuples "JSON" tuples, the templates should be "JSON" templates as well, being capable of matching them. This means
  * that all the available templates are made for covering a different part of the "JSON Schema" specification, the only
  * specification that can produce a JSON document which can be used for checking whether or not another JSON document conforms to
  * it. This means that the JSON Schema allows to create templates for JSON documents, which can be matched against, the
  * corresponding use case for this JSON template. Following the possible underlying representations of a JSON tuple and of an
  * element of the tuple itself, a template can match against a null value, a boolean, an integer, a long integer, a single
  * precision floating point number, a double precision floating point number, a string or a tuple itself. The JSON schema
  * specification allows also for creating templates of templates, where the single templates are used for matching using
  * different semantics. Then it can be possible to match a JSON element if any of the templates matches, if all of the templates
  * matches, if only one of the templates matches. It is also possible to match a template if the inner template does not match.
  */
sealed trait JsonTemplate {

  /** Returns whether or not the given [[JsonElement]] matches this template.
    *
    * @param value
    *   the [[JsonElement]] to match
    * @return
    *   whether or not the given [[JsonElement]] matches
    */
  def matches(value: JsonElement): Boolean
}

/** The companion object to the [[JsonTemplate]] trait, containing its implementations. */
object JsonTemplate {

  /** A [[JsonTemplate]] that matches a [[JsonElement]] based on multiple inner [[JsonTemplate]]s. */
  sealed trait JsonMultipleTemplate extends JsonTemplate {

      /** The [[JsonTemplate]]s to be used by this template to match the given [[JsonElement]]. */
    val templates: Seq[JsonTemplate]
  }

  /** Companion object to the [[JsonTemplate]] trait, containing its implementations. */
  object JsonMultipleTemplate {

    /** A [[JsonTemplate]] that matches a [[JsonElement]] if and only if any of its inner [[JsonTemplate]]s matches against the
      * element itself.
      */
    sealed trait JsonAnyOfTemplate extends JsonMultipleTemplate

      /** Companion object to the [[JsonAnyOfTemplate]] trait, containing its implementation. */
    object JsonAnyOfTemplate {

      /* Implementation of the JsonAnyOfTemplate trait. */
      final private case class JsonAnyOfTemplateImpl(templates: Seq[JsonTemplate]) extends JsonAnyOfTemplate {

        override def matches(value: JsonElement): Boolean = templates.exists(_.matches(value))
      }

      /** Factory method for building a new instance of the [[JsonAnyOfTemplate]] trait.
        *
        * @param templates
        *   the templates to be used by the [[JsonAnyOfTemplate]]
        * @return
        *   a new instance of the [[JsonAnyOfTemplate]] trait
        */
      def apply(templates: Seq[JsonTemplate]): JsonAnyOfTemplate = JsonAnyOfTemplateImpl(templates)
    }

    /** A [[JsonTemplate]] that matches a [[JsonElement]] if and only if all of its inner [[JsonTemplate]]s matches against the
      * element itself.
      */
    sealed trait JsonAllOfTemplate extends JsonMultipleTemplate

      /** Companion object to the [[JsonAllOfTemplate]] trait, containing its implementation. */
    object JsonAllOfTemplate {

      /* Implementation of the JsonAllOfTemplate trait. */
      final private case class JsonAllOfTemplateImpl(templates: Seq[JsonTemplate]) extends JsonAllOfTemplate {

        override def matches(value: JsonElement): Boolean = templates.forall(_.matches(value))
      }

      /** Factory method for building a new instance of the [[JsonAllOfTemplate]] trait.
        *
        * @param templates
        *   the templates to be used by the [[JsonAllOfTemplate]]
        * @return
        *   a new instance of the [[JsonAllOfTemplate]] trait
        */
      def apply(templates: Seq[JsonTemplate]): JsonAllOfTemplate = JsonAllOfTemplateImpl(templates)
    }

    /** A [[JsonTemplate]] that matches a [[JsonElement]] if and only if one of its inner [[JsonTemplate]]s matches against the
      * element itself.
      */
    sealed trait JsonOneOfTemplate extends JsonMultipleTemplate

      /** Companion object to the [[JsonOneOfTemplate]] trait, containing its implementation. */
    object JsonOneOfTemplate {

      /* Implementation of the JsonOneOfTemplate trait. */
      final private case class JsonOneOfTemplateImpl(templates: Seq[JsonTemplate]) extends JsonOneOfTemplate {

        override def matches(value: JsonElement): Boolean = templates.find(_.matches(value)) match {
          case Some(t) => templates.diff(Seq(t)).forall(ts => JsonNotTemplate(ts).matches(value))
          case _ => false
        }
      }

      /** Factory method for building a new instance of the [[JsonOneOfTemplate]] trait.
        *
        * @param templates
        *   the templates to be used by the [[JsonOneOfTemplate]]
        * @return
        *   a new instance of the [[JsonOneOfTemplate]] trait
        */
      def apply(templates: Seq[JsonTemplate]): JsonOneOfTemplate = JsonOneOfTemplateImpl(templates)
    }
  }

  /** A [[JsonTemplate]] that matches a [[JsonElement]] if and only if its inner [[JsonTemplate]]s does not match against the
    * element itself.
    */
  sealed trait JsonNotTemplate extends JsonTemplate {

      /** The [[JsonTemplate]] to be used by this template which should not match the given [[JsonElement]]. */
    val template: JsonTemplate
  }

  /** Companion object to the [[JsonNotTemplate]] trait, containing its implementation. */
  object JsonNotTemplate {

    /* Implementation of the JsonNotTemplate trait. */
    final private case class JsonNotTemplateImpl(template: JsonTemplate) extends JsonNotTemplate {

      override def matches(value: JsonElement): Boolean = !template.matches(value)
    }

    /** Factory method for building a new instance of the [[JsonNotTemplate]] trait.
      *
      * @param template
      *   the template to be used by the [[JsonNotTemplate]]
      * @return
      *   a new instance of the [[JsonNotTemplate]] trait
      */
    def apply(template: JsonTemplate): JsonNotTemplate = JsonNotTemplateImpl(template)
  }

  /** A [[JsonTemplate]] that matches a [[JsonTuple]].
    *
    * This template matches a [[JsonTuple]] if and only if its inner [[JsonTemplate]]s matches against the corresponding elements
    * of the tuple. Being a tuple in fact a representation of a JSON Array, the JSON Schema specification allows matching JSON
    * Arrays even if not all of its elements have a template to match against, but limits the matching abilities only to the first
    * ones, the ones which corresponds to inner templates. This is reflected allowing "additional items" to be present, so
    * allowing that this template can match even if the number of its inner [[JsonTemplate]]s is not equal to the arity of the
    * tuple, but it is inferior. A template with more inner templates than the tuple that is trying to match against will never
    * match.
    */
  sealed trait JsonTupleTemplate extends JsonTemplate {

      /** The [[JsonTemplate]]s to be used to match against the [[JsonElement]]s of the [[JsonTuple]] in their corresponding
        * positions.
        */
    val itemsTemplates: Seq[JsonTemplate]

    /** Whether or not the [[JsonTuple]] to be matched against is allowed to have more [[JsonElement]]s than the [[JsonTemplate]]s
      * contained in this template.
      */
    val additionalItems: Boolean
  }

  /** Companion object to the [[JsonTupleTemplate]] trait, containing its implementation. */
  object JsonTupleTemplate {

    /* Implementation of the JsonTupleTemplate trait. */
    final private case class JsonTupleTemplateImpl(itemsTemplates: Seq[JsonTemplate], additionalItems: Boolean)
      extends JsonTupleTemplate {

      override def matches(value: JsonElement): Boolean = value match {
        case es: JsonTuple =>
          ((es.arity === itemsTemplates.size) || (es.arity > itemsTemplates.size && additionalItems))
          && es.toSeq.zip(itemsTemplates).forall((e, t) => t.matches(e))
        case _ => false
      }
    }

    /** Factory method for building a new instance of the [[JsonTupleTemplate]] trait.
      *
      * @param itemsTemplates
      *   the [[JsonTemplate]]s to be used to match against the [[JsonElement]]s of the [[JsonTuple]] in their corresponding
      *   positions
      * @param additionalItems
      *   whether or not the [[JsonTuple]] to be matched against is allowed to have more [[JsonElement]]s than the
      *   [[JsonTemplate]]s contained in this template
      * @return
      *   a new instance of the [[JsonTupleTemplate]] trait
      */
    def apply(itemsTemplates: Seq[JsonTemplate], additionalItems: Boolean): JsonTupleTemplate =
      JsonTupleTemplateImpl(itemsTemplates, additionalItems)
  }

  /** A [[JsonTemplate]] that matches the <code>null</code> constant value. */
  @SuppressWarnings(Array("org.wartremover.warts.Null", "scalafix:DisableSyntax.null"))
  case object JsonNullTemplate extends JsonTemplate {

    override def matches(value: JsonElement): Boolean = value === null
  }

  /** A [[JsonTemplate]] that matches any value. */
  case object JsonAnyTemplate extends JsonTemplate {

    override def matches(value: JsonElement): Boolean = true
  }

  /** A [[JsonTemplate]] that matches a boolean value.
    *
    * A constant value can be specified to be matched against the given [[JsonElement]]. If not, only the type equality will be
    * checked.
    */
  sealed trait JsonBooleanTemplate extends JsonTemplate {

      /** The constant boolean value to be used for matching against the given [[JsonElement]]. */
    val const: Option[Boolean]
  }

  /** Companion object to the [[JsonBooleanTemplate]] trait, containing its implementation. */
  object JsonBooleanTemplate {

    /* Implementation of the JsonBooleanTemplate trait. */
    final private case class JsonBooleanTemplateImpl(const: Option[Boolean]) extends JsonBooleanTemplate {

      override def matches(value: JsonElement): Boolean = value match {
        case b: Boolean => const.forall(_ === b)
        case _ => false
      }
    }

    /** Factory method for building a new instance of the [[JsonBooleanTemplate]] trait.
      *
      * @param const
      *   the constant boolean value to be used for matching against the given [[JsonElement]]
      * @return
      *   a new instance of the [[JsonBooleanTemplate]] trait
      */
    def apply(const: Option[Boolean]): JsonBooleanTemplate = JsonBooleanTemplateImpl(const)
  }

  import scala.reflect.ClassTag

  /** A [[JsonTemplate]] that can be matched against a numeric value.
    *
    * This template represents all templates that can match a numeric value, so an integer, a long integer, a single precision
    * floating point value and a double precision floating point value. For matching a numeric [[JsonElement]], it can be
    * specified a constant value, an inclusive range for the [[JsonElement]] or an inclusive one, both of which can be half-open.
    * If multiple constraint are specified, all of them must be true in order to match with this template, independently of the
    * satisfiability of said constraints. If no constraint is specified, only the type equality will be checked.
    *
    * @tparam A
    *   the type of the numeric [[JsonElement]] that this [[JsonTemplate]] can match
    */
  sealed trait JsonNumericTemplate[+A: Numeric: ClassTag] extends JsonTemplate {

      /** The constant numeric value to be used for matching against the given [[JsonElement]]. */
    val const: Option[A]

    /** The inclusive minimum to be used for matching against the given [[JsonElement]]. */
    val minimum: Option[A]

    /** The inclusive maximum to be used for matching against the given [[JsonElement]]. */
    val maximum: Option[A]

    /** The exclusive minimum to be used for matching against the given [[JsonElement]]. */
    val exclusiveMinimum: Option[A]

    /** The exclusive maximum to be used for matching against the given [[JsonElement]]. */
    val exclusiveMaximum: Option[A]

    import math.Ordering.Implicits.infixOrderingOps

    override def matches(value: JsonElement): Boolean = value match {
      case n: A =>
        const.forall(_ === n)
        && minimum.forall(_ <= n)
        && maximum.forall(_ >= n)
        && exclusiveMinimum.forall(_ < n)
        && exclusiveMaximum.forall(_ > n)
      case _ => false
    }
  }

  /** Companion object to the [[JsonNumericTemplate]] trait, containing its implementations. */
  object JsonNumericTemplate {

    /** A [[JsonTemplate]] that can be matched against a single precision floating point numeric value. */
    sealed trait JsonFloatTemplate extends JsonNumericTemplate[Float]

      /** Companion object to the [[JsonFloatTemplate]] trait, containing its implementation. */
    object JsonFloatTemplate {

      /* Implementation of the JsonFloatTemplate trait. */
      final private case class JsonFloatTemplateImpl(
        const: Option[Float],
        minimum: Option[Float],
        maximum: Option[Float],
        exclusiveMinimum: Option[Float],
        exclusiveMaximum: Option[Float]
      ) extends JsonFloatTemplate

        /** Factory method for building a new instance of the [[JsonFloatTemplate]] trait.
          *
          * @param const
          *   the constant numeric value to be used for matching against the given [[JsonElement]]
          * @param minimum
          *   the inclusive minimum to be used for matching against the given [[JsonElement]]
          * @param maximum
          *   the inclusive maximum to be used for matching against the given [[JsonElement]]
          * @param exclusiveMinimum
          *   the exclusive minimum to be used for matching against the given [[JsonElement]]
          * @param exclusiveMaximum
          *   the exclusive maximum to be used for matching against the given [[JsonElement]]
          * @return
          *   a new instance of the [[JsonFloatTemplate]] trait
          */
      def apply(
        const: Option[Float],
        minimum: Option[Float],
        maximum: Option[Float],
        exclusiveMinimum: Option[Float],
        exclusiveMaximum: Option[Float]
      ): JsonFloatTemplate = JsonFloatTemplateImpl(const, minimum, maximum, exclusiveMinimum, exclusiveMaximum)
    }

    /** A [[JsonTemplate]] that can be matched against a double precision floating point numeric value. */
    sealed trait JsonDoubleTemplate extends JsonNumericTemplate[Double]

      /** Companion object to the [[JsonDoubleTemplate]] trait, containing its implementation. */
    object JsonDoubleTemplate {

      /* Implementation of the JsonDoubleTemplate trait. */
      final private case class JsonDoubleTemplateImpl(
        const: Option[Double],
        minimum: Option[Double],
        maximum: Option[Double],
        exclusiveMinimum: Option[Double],
        exclusiveMaximum: Option[Double]
      ) extends JsonDoubleTemplate

        /** Factory method for building a new instance of the [[JsonDoubleTemplate]] trait.
          *
          * @param const
          *   the constant numeric value to be used for matching against the given [[JsonElement]]
          * @param minimum
          *   the inclusive minimum to be used for matching against the given [[JsonElement]]
          * @param maximum
          *   the inclusive maximum to be used for matching against the given [[JsonElement]]
          * @param exclusiveMinimum
          *   the exclusive minimum to be used for matching against the given [[JsonElement]]
          * @param exclusiveMaximum
          *   the exclusive maximum to be used for matching against the given [[JsonElement]]
          * @return
          *   a new instance of the [[JsonDoubleTemplate]] trait
          */
      def apply(
        const: Option[Double],
        minimum: Option[Double],
        maximum: Option[Double],
        exclusiveMinimum: Option[Double],
        exclusiveMaximum: Option[Double]
      ): JsonDoubleTemplate = JsonDoubleTemplateImpl(const, minimum, maximum, exclusiveMinimum, exclusiveMaximum)
    }

    /** A [[JsonTemplate]] that can be matched against an integral value.
      *
      * This template represents all templates that can match an integral value, so an integer or a long integer. For matching an
      * integral [[JsonElement]], it can be specified a constant value, an inclusive range for the [[JsonElement]] or an inclusive
      * one, both of which can be half-open. Also, it can be specified a value for which the [[JsonElement]] should be multiple
      * of. If multiple constraint are specified, all of them must be true in order to match with this template, independently of
      * the satisfiability of said constraints. If no constraint is specified, only the type equality will be checked.
      *
      * @tparam A
      *   the type of the integral [[JsonElement]] that this [[JsonTemplate]] can match
      */
    sealed trait JsonIntegralTemplate[+A: Integral: ClassTag] extends JsonNumericTemplate[A] {

        /** The value for which the given [[JsonElement]] should be a multiple of. */
      val multipleOf: Option[A]

      import math.Integral.Implicits.infixIntegralOps

      override def matches(value: JsonElement): Boolean = super.matches(value) && (value match {
        case i: A => multipleOf.forall(i % _ === 0)
        case _ => false
      })
    }

    /** Companion object to the [[JsonIntegralTemplate]] trait, containing its implementation. */
    object JsonIntegralTemplate {

      /** A [[JsonTemplate]] that can be matched against an integer numeric value. */
      sealed trait JsonIntTemplate extends JsonIntegralTemplate[Int]

        /** Companion object to the [[JsonIntTemplate]] trait, containing its implementation. */
      object JsonIntTemplate {

        /* Implementation of the JsonIntTemplate trait. */
        final private case class JsonIntTemplateImpl(
          const: Option[Int],
          multipleOf: Option[Int],
          minimum: Option[Int],
          maximum: Option[Int],
          exclusiveMinimum: Option[Int],
          exclusiveMaximum: Option[Int]
        ) extends JsonIntTemplate

          /** Factory method for building a new instance of the [[JsonDoubleTemplate]] trait.
            *
            * @param const
            *   the constant numeric value to be used for matching against the given [[JsonElement]]
            * @param multipleOf
            *   the value for which the given [[JsonElement]] should be a multiple of
            * @param minimum
            *   the inclusive minimum to be used for matching against the given [[JsonElement]]
            * @param maximum
            *   the inclusive maximum to be used for matching against the given [[JsonElement]]
            * @param exclusiveMinimum
            *   the exclusive minimum to be used for matching against the given [[JsonElement]]
            * @param exclusiveMaximum
            *   the exclusive maximum to be used for matching against the given [[JsonElement]]
            * @return
            *   a new instance of the [[JsonDoubleTemplate]] trait
            */
        def apply(
          const: Option[Int],
          multipleOf: Option[Int],
          minimum: Option[Int],
          maximum: Option[Int],
          exclusiveMinimum: Option[Int],
          exclusiveMaximum: Option[Int]
        ): JsonIntTemplate = JsonIntTemplateImpl(
          const,
          multipleOf,
          minimum,
          maximum,
          exclusiveMinimum,
          exclusiveMaximum
        )
      }

      /** A [[JsonTemplate]] that can be matched against a long integer numeric value. */
      sealed trait JsonLongTemplate extends JsonIntegralTemplate[Long]

        /** Companion object to the [[JsonLongTemplate]] trait, containing its implementation. */
      object JsonLongTemplate {

        /* Implementation of the JsonLongTemplate trait. */
        final private case class JsonLongTemplateImpl(
          const: Option[Long],
          multipleOf: Option[Long],
          minimum: Option[Long],
          maximum: Option[Long],
          exclusiveMinimum: Option[Long],
          exclusiveMaximum: Option[Long]
        ) extends JsonLongTemplate

          /** Factory method for building a new instance of the [[JsonLongTemplate]] trait.
            *
            * @param const
            *   the constant numeric value to be used for matching against the given [[JsonElement]]
            * @param multipleOf
            *   the value for which the given [[JsonElement]] should be a multiple of
            * @param minimum
            *   the inclusive minimum to be used for matching against the given [[JsonElement]]
            * @param maximum
            *   the inclusive maximum to be used for matching against the given [[JsonElement]]
            * @param exclusiveMinimum
            *   the exclusive minimum to be used for matching against the given [[JsonElement]]
            * @param exclusiveMaximum
            *   the exclusive maximum to be used for matching against the given [[JsonElement]]
            * @return
            *   a new instance of the [[JsonLongTemplate]] trait
            */
        def apply(
          const: Option[Long],
          multipleOf: Option[Long],
          minimum: Option[Long],
          maximum: Option[Long],
          exclusiveMinimum: Option[Long],
          exclusiveMaximum: Option[Long]
        ): JsonLongTemplate = JsonLongTemplateImpl(const, multipleOf, minimum, maximum, exclusiveMinimum, exclusiveMaximum)
      }
    }

    export JsonIntegralTemplate.*
  }

  import scala.util.matching.Regex

  /** A [[JsonTemplate]] that can be matched against a string value.
    *
    * A [[JsonElement]] can be matched using a set of strings, creating what is called in the JSON Schema an "enum". Regardless of
    * this, specifying a [[Set]] constrains the values that the [[JsonElement]] can have, because no other string value outside of
    * the [[Set]] could be accepted. If the [[Set]] is the singleton [[Set]], this constraint is perfectly equal to specifying a
    * constant value to be matched against. The given [[JsonElement]] can be constrained in terms of an inclusive range of string
    * length, which can be half-open, and in terms of the regular expression that can match it. If no constraint is specified,
    * only the type equality will be checked.
    */
  sealed trait JsonStringTemplate extends JsonTemplate {

      /** The [[Set]] of values that the [[JsonElement]] can assume when is to be matched. */
    val values: Option[Set[String]]

    /** The inclusive minimum length of the [[JsonElement]] that is to be matched. */
    val minLength: Option[Int]

    /** The inclusive maximum length of the [[JsonElement]] that is to be matched. */
    val maxLength: Option[Int]

    /** The regular expression matching the [[JsonElement]] that is to be matched. */
    val pattern: Option[Regex]
  }

  /** Companion object to the [[JsonStringTemplate]] trait, containing its implementation. */
  object JsonStringTemplate {

    /* Implementation of the JsonIntTemplate trait. */
    final private case class JsonStringTemplateImpl(
      values: Option[Set[String]],
      minLength: Option[Int],
      maxLength: Option[Int],
      pattern: Option[Regex]
    ) extends JsonStringTemplate {

      override def matches(value: JsonElement): Boolean = value match {
        case s: String =>
          values.forall(_.apply(s))
          && minLength.forall(_ <= s.length)
          && maxLength.forall(_ >= s.length)
          && pattern.forall(_.matches(s))
        case _ => false
      }

      @SuppressWarnings(Array("org.wartremover.warts.IsInstanceOf", "scalafix:DisableSyntax.isInstanceOf"))
      override def canEqual(that: Any): Boolean = that.isInstanceOf[JsonStringTemplateImpl]

      override def equals(obj: Any): Boolean = obj match {
        case t: JsonStringTemplateImpl =>
          t.canEqual(this)
          && values === t.values
          && minLength === t.minLength
          && maxLength === t.maxLength
          && pattern.map(_.pattern.pattern) === t.pattern.map(_.pattern.pattern)
        case _ => false
      }

      override def hashCode(): Int = Objects.hash(values, minLength, maxLength, pattern.map(_.pattern.pattern))
    }

    /** Factory method for building a new instance of the [[JsonStringTemplate]] trait.
      *
      * @param values
      *   the [[Set]] of values that the [[JsonElement]] can assume when is to be matched
      * @param minLength
      *   the inclusive minimum length of the [[JsonElement]] that is to be matched
      * @param maxLength
      *   the inclusive maximum length of the [[JsonElement]] that is to be matched
      * @param pattern
      *   the regular expression matching the [[JsonElement]] that is to be matched
      * @return
      *   a new instance of the [[JsonStringTemplate]] trait
      */
    def apply(
      values: Option[Set[String]],
      minLength: Option[Int],
      maxLength: Option[Int],
      pattern: Option[Regex]
    ): JsonStringTemplate = JsonStringTemplateImpl(values, minLength, maxLength, pattern)
  }

  export JsonMultipleTemplate.*
  export JsonNumericTemplate.*
}

export JsonTemplate.*
