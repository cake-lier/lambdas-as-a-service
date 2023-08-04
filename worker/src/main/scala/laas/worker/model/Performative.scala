package io.github.cakelier
package laas.worker.model

/**
 * The communicative acts that the agents can perform while communicating between them.
 * @see http://www.fipa.org/specs/fipa00037/SC00037J.html
 */
enum Performative {

  /**
   * The action of accepting a previously submitted proposal to perform an action.
   */
  case AcceptProposal extends Performative

  /**
   * The action of one agent informing another agent that the first agent no longer has the intention that the second agent
   * performs some action.
   */
  case Cancel extends Performative

  /**
   * The action of calling for proposals to perform a given action.
   */
  case Cfp extends Performative

  /**
   * The action of telling another agent that an action was attempted but the attempt failed.
   */
  case Failure extends Performative

  /**
   * The sender informs the receiver that a given proposition is true. That is, the performed action has completed with success.
   */
  case InformDone extends Performative

  /**
   * The sender informs the receiver that a given proposition is true. That is, the performed action has completed with success
   * and the message content contains the result of this action.
   */
  case InformResult extends Performative

  /**
   * The action of submitting a proposal to perform a certain action, given certain preconditions.
   */
  case Propose extends Performative

  /**
   * The action of refusing to perform a given action, and explaining the reason for the refusal.
   */
  case Refuse extends Performative

  /**
   * The sender requests the receiver to perform some action.
   */
  case Request extends Performative

  /**
   * The action of rejecting a proposal to perform some action during a negotiation.
   */
  case RejectProposal extends Performative
}
