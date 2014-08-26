package akka.actor

import scala.annotation.tailrec

/** Base class for actors exceptions. */
class ActorsException(message: String,
    cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}

/**
 * IllegalActorStateException is thrown when a core invariant in the Actor
 * implementation has been violated.
 * For instance, if you try to create an Actor that doesn't extend Actor.
 */
final case class IllegalActorStateException private[akka] (
    message: String) extends ActorsException(message)

/**
 * ActorKilledException is thrown when an Actor receives the
 * [[org.scalajs.actors.Kill]] message
 */
final case class ActorKilledException private[akka] (
    message: String) extends ActorsException(message)

/**
 * An InvalidActorNameException is thrown when you try to convert something,
 * usually a String, to an Actor name which doesn't validate.
 */
final case class InvalidActorNameException(
    message: String) extends ActorsException(message)

/**
 * An ActorInitializationException is thrown when the the initialization logic
 * for an Actor fails.
 *
 * There is an extractor which works for ActorInitializationException and its
 * subtypes:
 *
 * {{{
 * ex match {
 *   case ActorInitializationException(actor, message, cause) => ...
 * }
 * }}}
 */
class ActorInitializationException protected (actor: ActorRef,
    message: String, cause: Throwable)
    extends ActorsException(message, cause) {
  def getActor(): ActorRef = actor
}

object ActorInitializationException {
  private[akka] def apply(actor: ActorRef, message: String, cause: Throwable = null): ActorInitializationException =
    new ActorInitializationException(actor, message, cause)
  private[akka] def apply(message: String): ActorInitializationException =
    new ActorInitializationException(null, message, null)
  def unapply(ex: ActorInitializationException): Option[(ActorRef, String, Throwable)] =
    Some((ex.getActor, ex.getMessage, ex.getCause))
}
