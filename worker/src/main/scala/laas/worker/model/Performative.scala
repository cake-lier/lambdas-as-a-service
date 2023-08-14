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
package laas.worker.model

import AnyOps.*

/** The communicative acts that the agents can perform while communicating between them.
  * @see
  *   http://www.fipa.org/specs/fipa00037/SC00037J.html
  */
enum Performative(val name: String) {

  /** The action of accepting a previously submitted proposal to perform an action. */
  case AcceptProposal extends Performative("accept-proposal")

    /** The action of one agent informing another agent that the first agent no longer has the intention that the second agent
      * performs some action.
      */
  case Cancel extends Performative("cancel")

    /** The action of calling for proposals to perform a given action. */
  case Cfp extends Performative("cfp")

    /** The action of telling another agent that an action was attempted but the attempt failed. */
  case Failure extends Performative("failure")

    /** The sender informs the receiver that a given proposition is true. That is, the performed action has completed with
      * success.
      */
  case InformDone extends Performative("inform-done")

    /** The sender informs the receiver that a given proposition is true. That is, the performed action has completed with success
      * and the message content contains the result of this action.
      */
  case InformResult extends Performative("inform-result")

    /** The action of submitting a proposal to perform a certain action, given certain preconditions. */
  case Propose extends Performative("propose")

    /** The action of refusing to perform a given action, and explaining the reason for the refusal. */
  case Refuse extends Performative("refuse")

    /** The sender requests the receiver to perform some action. */
  case Request extends Performative("request")

    /** The action of rejecting a proposal to perform some action during a negotiation. */
  case RejectProposal extends Performative("reject-proposal")

    /** Returns the correct performative given the name of the performative itself.
      *
      * @param name
      *   the name of the performative to get
      * @return
      *   the correct performative given the name of the performative itself
      */
  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def fromName(name: String): Performative = Performative.values.find(_.name === name).get
}
