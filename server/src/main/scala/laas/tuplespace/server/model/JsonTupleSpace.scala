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
package laas.tuplespace.server.model

import java.util.UUID

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.Promise

import laas.AnyOps.===
import laas.tuplespace.*

/** A coordination medium to be used to exchange pieces of information and coordinate with other entities, implemented to be used
  * with [[JsonTuple]]s and [[JsonTemplate]]s.
  *
  * A "tuple space", in general, is what is called a coordination medium. It is a way to coordinate the entities that participate
  * in its use. It is a way to exchange information, more specifically tuples and with this implementation [[JsonTuple]]s, and to
  * coordinate the different actions of the entities. For a "tuple space", the coordination happens with the same operations that
  * let to write and to read into the space. The basic operations, "in" and "rd", have a suspensive semantic, which means that
  * their completion suspends until a tuple able to complete them is found in the space. In this way, similarly to the "future"
  * data structure, the execution can be paused until the result is ready. Matching a tuple means to have a template to be used by
  * the operation for matching, which in this implementation is a [[JsonTemplate]].
  */
private[server] trait JsonTupleSpace {

  /** The operation for inserting a [[JsonTuple]] into this [[JsonTupleSpace]]. This is one of the core operations on the tuple
    * space, along with "in" and "rd". Differently from these two, this operation is not suspensive: it completes right away,
    * because it is always allowed to insert a new tuple into the space.
    *
    * @param t
    *   the [[JsonTuple]] to be inserted into this [[JsonTupleSpace]]
    */
  def out(t: JsonTuple): Unit

  /** The operation for reading a [[JsonTuple]] into this [[JsonTupleSpace]]. This is one of the core operations on the tuple
    * space, along with "in" and "out". This is a suspensive operation, it will complete only when in the space a tuple matching
    * the template of this operation is found, hence the returned type which is a [[Future]]. This also mean that the operation
    * will not suspend at all, if a tuple is already inside the space. If multiple tuples matching the template are inside the
    * space, one will be chosen randomly, following the "don't care" nondeterminism. The tuple matched is not removed from the
    * tuple space. The id of the operation must also be given, in order to undo this operation while is still pending.
    *
    * @param tt
    *   the [[JsonTemplate]] to be used for matching a [[JsonTuple]] to be read in this [[JsonTupleSpace]]
    * @param id
    *   the id of the operation, to be used for identifying it while deciding if removing it or not
    * @return
    *   a [[Future]] which completes when the operation has completed with the read [[JsonTuple]]
    */
  def rd(tt: JsonTemplate, id: UUID): Future[JsonTuple]

  /** The operation for taking a [[JsonTuple]] from this [[JsonTupleSpace]]. This is one of the core operations on the tuple
    * space, along with "rd" and "out". This is a suspensive operation, it will complete only when in the space a tuple matching
    * the template of this operation is found, hence the returned type which is a [[Future]]. This also mean that the operation
    * will not suspend at all, if a tuple is already inside the space. If multiple tuples matching the template are inside the
    * space, one will be chosen randomly, following the "don't care" nondeterminism. The tuple matched is then removed from the
    * tuple space. The id of the operation must also be given, in order to undo this operation while is still pending.
    *
    * @param tt
    *   the [[JsonTemplate]] to be used for matching a [[JsonTuple]] to be taken in this [[JsonTupleSpace]]
    * @param id
    *   the id of the operation, to be used for identifying it while deciding if removing it or not
    * @return
    *   a [[Future]] which completes when the operation has completed with the taken [[JsonTuple]]
    */
  def in(tt: JsonTemplate, id: UUID): Future[JsonTuple]

  /** The operation for checking if some [[JsonTuple]]s are not into this [[JsonTupleSpace]]. This is a suspensive operation, it
    * will complete only when in the space no tuple matching the template of this operation is found, hence the return type which
    * is a [[Future]]. This also mean that the operation will not suspend at all, if no tuple is already inside the space. If
    * multiple tuples matching the template are inside the space, only when the last one is removed the operation will complete.
    * The id of the operation must also be given, in order to undo this operation while is still pending.
    *
    * @param tt
    *   the [[JsonTemplate]] to be used for matching [[JsonTuple]]s which should not be in this [[JsonTupleSpace]]
    * @param id
    *   the id of the operation, to be used for identifying it while deciding if removing it or not
    * @return
    *   a [[Future]] which completes when the operation has completed
    */
  def no(tt: JsonTemplate, id: UUID): Future[Unit]

  /** The operation for inserting multiple [[JsonTuple]]s into this [[JsonTupleSpace]]. This is the "bulk" version of the basic
    * "out" operation. As for its basic counterpart, this operation is not suspensive: it completes right away, because it is
    * always allowed to insert new tuples into the space. A [[Future]] is still returned because the actual tuple space can be
    * hosted on a remote host, meaning that the operation is in fact a network operation that takes time to complete. The future
    * will complete signalling only the success of the operation.
    *
    * @param ts
    *   the [[JsonTuple]]s to be inserted into this [[JsonTupleSpace]]
    */
  def outAll(ts: JsonTuple*): Unit

  /** The operation for reading some [[JsonTuple]]s into this [[JsonTupleSpace]]. This is the "bulk" version of the basic "rd"
    * operation. Differently from its basic counterpart, this is not a suspensive operation, if no tuples matching the given
    * template are found, an empty [[Seq]] is returned. If multiple tuples matching the template are inside the space, all will be
    * returned in a [[Seq]]. The tuples matched are not removed from the tuple space.
    *
    * @param tt
    *   the [[JsonTemplate]] to be used for matching some [[JsonTuple]]s to be read in this [[JsonTupleSpace]]
    * @return
    *   the [[Seq]] of the read [[JsonTuple]]s
    */
  def rdAll(tt: JsonTemplate): Seq[JsonTuple]

  /** The operation for taking some [[JsonTuple]]s from this [[JsonTupleSpace]]. This is the "bulk" version of the basic "in"
    * operation. Differently from its basic counterpart, this is not a suspensive operation, if no tuples matching the given
    * template are found, an empty [[Seq]] is returned. If multiple tuples matching the template are inside the space, all will be
    * returned in a [[Seq]]. The tuples matched are removed from the tuple space.
    *
    * @param tt
    *   the [[JsonTemplate]] to be used for matching some [[JsonTuple]]s to be taken from this [[JsonTupleSpace]]
    * @return
    *   the [[Seq]] of the taken [[JsonTuple]]s
    */
  def inAll(tt: JsonTemplate): Seq[JsonTuple]

  /** The operation for reading a [[JsonTuple]] into this [[JsonTupleSpace]]. This is the "predicative" version of the basic "rd"
    * operation: this means that it's not a suspensive operation. If no tuples matching the given template are found, a [[None]]
    * is returned. If multiple tuples matching the template are inside the space, one will be chosen randomly, following the
    * "don't care" nondeterminism. The tuple matched is not removed from the tuple space. If a tuple is matched, will be returned
    * wrapped in a [[Some]].
    *
    * @param tt
    *   the [[JsonTemplate]] to be used for matching a [[JsonTuple]] to be read in this [[JsonTupleSpace]]
    * @return
    *   a [[Some]] containing the read [[JsonTuple]], if present, a [[None]] otherwise
    */
  def rdp(tt: JsonTemplate): Option[JsonTuple]

  /** The operation for taking a [[JsonTuple]] from this [[JsonTupleSpace]]. This is the "predicative" version of the basic "in"
    * operation: this means that it's not a suspensive operation. If no tuples matching the given template are found, a [[None]]
    * is returned. If multiple tuples matching the template are inside the space, one will be chosen randomly, following the
    * "don't care" nondeterminism. The tuple matched is then removed from the tuple space. If a tuple is matched, will be returned
    * wrapped in a [[Some]].
    *
    * @param tt
    *   the [[JsonTemplate]] to be used for matching a [[JsonTuple]] to be taken from this [[JsonTupleSpace]]
    * @return
    *   a [[Some]] containing the taken [[JsonTuple]], if present, a [[None]] otherwise
    */
  def inp(tt: JsonTemplate): Option[JsonTuple]

  /** The operation for checking if some [[JsonTuple]]s are not into this [[JsonTupleSpace]]. This is the "predicative" version of
    * the basic "no" operation: this means that it's not a suspensive operation. If no tuples matching the given template are
    * found, <code>true</code> is returned, otherwise <code>false</code> is returned. The tuples matched are not removed from the
    * tuple space.
    *
    * @param tt
    *   the [[JsonTemplate]] to be used for matching [[JsonTuple]]s which should not be in this [[JsonTupleSpace]]
    * @return
    *   <code>true</code> if no [[JsonTuple]]s matching the [[JsonTemplate]] are found, <code>false</code> otherwise
    */
  def nop(tt: JsonTemplate): Boolean

  /** Removes all pending operations with the given id. It is not mandatory that the id is unique for each operation, but if this
    * is not the case, all pending operations with the same id will be removed, if their id is equal to the one passed.
    *
    * @param id
    *   the id of the pending operations to be deleted
    */
  def remove(id: UUID): Unit
}

/** Companion object to the [[JsonTupleSpace]] trait, containing its factory method. */
object JsonTupleSpace {

  /* The types of pending requests which are not the "no" operation. */
  private enum PendingRequestType {

    case In extends PendingRequestType

    case Rd extends PendingRequestType
  }

  /* The type of a "in" or "rd" pending request. */
  private type InRdRequest = (UUID, JsonTemplate, PendingRequestType, Promise[JsonTuple])
  /* The type of a "no" pending request. */
  private type NoRequest = (UUID, JsonTemplate, Promise[Unit])

  /* The implementation of the JsonTupleSpace trait. */
  @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
  private class JsonTupleSpaceImpl extends JsonTupleSpace {

    private var pendingTuples: Seq[JsonTuple] = Seq.empty
    private var pendingInRdRequests: Seq[InRdRequest] = Seq.empty
    private var pendingNoRequests: Seq[NoRequest] = Seq.empty

    private def completeInRdRequests(tuple: JsonTuple): Unit = {
      @tailrec
      def _completeInRdRequests(remainingInRdRequests: Seq[InRdRequest], newInRdRequests: Seq[InRdRequest]): Unit =
        remainingInRdRequests match {
          case (r @ (_, template, tpe, promise)) +: rs =>
            tpe match {
              case PendingRequestType.In if template.matches(tuple) =>
                promise.success(tuple)
                pendingInRdRequests = newInRdRequests :++ rs
              case PendingRequestType.Rd if template.matches(tuple) =>
                promise.success(tuple)
                _completeInRdRequests(rs, newInRdRequests)
              case _ => _completeInRdRequests(rs, newInRdRequests :+ r)
            }
          case _ =>
            pendingTuples :+= tuple
            pendingInRdRequests = newInRdRequests
        }
      _completeInRdRequests(pendingInRdRequests, Seq.empty)
    }

    override def out(t: JsonTuple): Unit = completeInRdRequests(t)

    private def completeNoRequests(tuplesToRemove: Seq[JsonTuple]): Unit = {
      pendingTuples = pendingTuples.diff(tuplesToRemove)
      val completableNoRequests = pendingNoRequests.filter((_, tt, _) => pendingTuples.forall(t => !tt.matches(t)))
      completableNoRequests.foreach((_, _, p) => p.success(()))
      pendingNoRequests = pendingNoRequests.diff(completableNoRequests)
    }

    override def in(tt: JsonTemplate, id: UUID): Future[JsonTuple] = {
      val promise: Promise[JsonTuple] = Promise()
      pendingTuples
        .find(tt.matches)
        .fold {
          pendingInRdRequests = pendingInRdRequests :+ (id, tt, PendingRequestType.In, promise)
        }(t => {
          promise.success(t)
          completeNoRequests(Seq(t))
        })
      promise.future
    }

    override def rd(tt: JsonTemplate, id: UUID): Future[JsonTuple] = {
      val promise: Promise[JsonTuple] = Promise()
      pendingTuples
        .find(tt.matches)
        .fold {
          pendingInRdRequests = pendingInRdRequests :+ (id, tt, PendingRequestType.Rd, promise)
        }(t => promise.success(t))
      promise.future
    }

    override def no(tt: JsonTemplate, id: UUID): Future[Unit] = {
      val promise: Promise[Unit] = Promise()
      if (pendingTuples.forall(t => !tt.matches(t))) {
        promise.success(())
      } else {
        pendingNoRequests = pendingNoRequests :+ (id, tt, promise)
      }
      promise.future
    }

    override def inp(tt: JsonTemplate): Option[JsonTuple] =
      pendingTuples
        .find(tt.matches)
        .map(t => {
          completeNoRequests(Seq(t))
          t
        })

    override def rdp(tt: JsonTemplate): Option[JsonTuple] = pendingTuples.find(tt.matches)

    override def nop(tt: JsonTemplate): Boolean = pendingTuples.forall(t => !tt.matches(t))

    override def outAll(ts: JsonTuple*): Unit = ts.foreach(completeInRdRequests)

    override def inAll(tt: JsonTemplate): Seq[JsonTuple] = {
      val tuples = pendingTuples.filter(tt.matches)
      completeNoRequests(tuples)
      tuples
    }

    override def rdAll(tt: JsonTemplate): Seq[JsonTuple] = pendingTuples.filter(tt.matches)

    override def remove(id: UUID): Unit = {
      pendingInRdRequests = pendingInRdRequests.filterNot((i, _, _, _) => i === id)
      pendingNoRequests = pendingNoRequests.filterNot((i, _, _) => i === id)
    }
  }

  /** Creates a new instance of the [[JsonTupleSpace]] trait. */
  def apply(): JsonTupleSpace = JsonTupleSpaceImpl()
}
