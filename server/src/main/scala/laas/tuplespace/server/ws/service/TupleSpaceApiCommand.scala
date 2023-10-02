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
package laas.tuplespace.server.ws.service

import laas.tuplespace.*
import laas.tuplespace.server.TupleSpaceApi
import laas.tuplespace.server.ws.presentation.response.Response

import akka.actor.typed.ActorRef

import java.util.UUID

/** The enum representing all possible messages that can be sent to a [[TupleSpaceApi]].
  *
  * These messages can be either messages for requesting specific operations to be performed on the tuple space that the
  * [[TupleSpaceApi]] is managing or messages for handling the entrance and the exiting of a client.
  */
private[server] enum TupleSpaceApiCommand {

  /** The message signalling that the client with the given id wants to perform the "out" operation with the given [[JsonTuple]]
    * on the tuple space that the [[TupleSpaceApi]] is managing.
    *
    * @constructor
    *   creates a new message given the [[JsonTuple]] to be used in the "out" operation and the actor to which signal the
    *   completion of the operation
    */
  case Out(tuple: JsonTuple, id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client with the given id wants to perform the "in" operation with the given
      * [[JsonTemplate]] on the tuple space that the [[TupleSpaceApi]] is managing.
      *
      * @constructor
      *   creates a new message given the [[JsonTemplate]] to be used in the "in" operation and the actor to which signal the
      *   completion of the operation
      */
  case In(template: JsonTemplate, id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client with the given id wants to perform the "rd" operation with the given
      * [[JsonTemplate]] on the tuple space that the [[TupleSpaceApi]] is managing.
      *
      * @constructor
      *   creates a new message given the [[JsonTemplate]] to be used in the "rd" operation and the actor to which signal the
      *   completion of the operation
      */
  case Rd(template: JsonTemplate, id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client with the given id wants to perform the "no" operation with the given
      * [[JsonTemplate]] on the tuple space that the [[TupleSpaceApi]] is managing.
      *
      * @constructor
      *   creates a new message given the [[JsonTemplate]] to be used in the "no" operation and the actor to which signal the
      *   completion of the operation
      */
  case No(template: JsonTemplate, id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client with the given id wants to perform the "outAll" operation with the given [[Seq]] of
      * [[JsonTuple]]s on the tuple space that the [[TupleSpaceApi]] is managing.
      *
      * @constructor
      *   creates a new message given the [[Seq]] of [[JsonTuple]]s to be used in the "outAll" operation and the actor to which
      *   signal the completion of the operation
      */
  case OutAll(tuples: Seq[JsonTuple], id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client with the given id wants to perform the "inAll" operation with the given
      * [[JsonTemplate]] on the tuple space that the [[TupleSpaceApi]] is managing.
      *
      * @constructor
      *   creates a new message given the [[JsonTemplate]] to be used in the "inAll" operation and the actor to which signal the
      *   completion of the operation
      */
  case InAll(template: JsonTemplate, id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client with the given id wants to perform the "rdAll" operation with the given
      * [[JsonTemplate]] on the tuple space that the [[TupleSpaceApi]] is managing.
      *
      * @constructor
      *   creates a new message given the [[JsonTemplate]] to be used in the "rdAll" operation and the actor to which signal the
      *   completion of the operation
      */
  case RdAll(template: JsonTemplate, id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client with the given id wants to perform the "inp" operation with the given
      * [[JsonTemplate]] on the tuple space that the [[TupleSpaceApi]] is managing.
      *
      * @constructor
      *   creates a new message given the [[JsonTemplate]] to be used in the "inp" operation and the actor to which signal the
      *   completion of the operation
      */
  case Inp(template: JsonTemplate, id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client with the given id wants to perform the "rdp" operation with the given
      * [[JsonTemplate]] on the tuple space that the [[TupleSpaceApi]] is managing.
      *
      * @constructor
      *   creates a new message given the [[JsonTemplate]] to be used in the "rdp" operation and the actor to which signal the
      *   completion of the operation
      */
  case Rdp(template: JsonTemplate, id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client with the given id wants to perform the "nop" operation with the given
      * [[JsonTemplate]] on the tuple space that the [[TupleSpaceApi]] is managing.
      *
      * @constructor
      *   creates a new message given the [[JsonTemplate]] to be used in the "nop" operation and the actor to which signal the
      *   completion of the operation
      */
  case Nop(template: JsonTemplate, id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that a new client has joined the tuple space and all responses to its requests must be routed to
      * the given actor.
      *
      * @constructor
      *   creates a new message given the actor to which route all responses for the new client
      */
  case Enter(actorRef: ActorRef[Response], id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client already connected has requested to change its id to its old one, which had before a
      * disconnection, in order to recover all the pending requests still not satisfied.
      *
      * @constructor
      *   creates a new message given the old id that the client wants to be associated with and the actor to which signal the
      *   completion of the operation
      */
  case MergeIds(oldId: UUID, id: UUID) extends TupleSpaceApiCommand

    /** The message signalling that the client has left the tuple space. If the exiting was not forced, so no abrupt disconnection
      * followed with an error, then it is considered an exiting with success.
      *
      * @constructor
      *   creates a new message given the exit status and the actor which represents the exited client
      */
  case Exit(success: Boolean, id: UUID) extends TupleSpaceApiCommand
}
