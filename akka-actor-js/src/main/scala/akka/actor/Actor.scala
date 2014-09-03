package akka.actor

import akka.AkkaException
import scala.annotation.unchecked.uncheckedStable
import scala.annotation.tailrec

/**
 * IllegalActorStateException is thrown when a core invariant in the Actor
 * implementation has been violated.
 * For instance, if you try to create an Actor that doesn't extend Actor.
 */
final case class IllegalActorStateException private[akka] (
  message: String) extends AkkaException(message)

/**
 * ActorKilledException is thrown when an Actor receives the
 * [[org.scalajs.actors.Kill]] message
 */
final case class ActorKilledException private[akka] (
  message: String) extends AkkaException(message)

/**
 * An InvalidActorNameException is thrown when you try to convert something,
 * usually a String, to an Actor name which doesn't validate.
 */
final case class InvalidActorNameException(
  message: String) extends AkkaException(message)

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
  extends AkkaException(message, cause) {
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

/**
 * A PreRestartException is thrown when the preRestart() method failed; this
 * exception is not propagated to the supervisor, as it originates from the
 * already failed instance, hence it is only visible as log entry on the event
 * stream.
 *
 * @param actor is the actor whose preRestart() hook failed
 * @param cause is the exception thrown by that actor within preRestart()
 * @param originalCause is the exception which caused the restart in the first place
 * @param messageOption is the message which was optionally passed into preRestart()
 */
final case class PreRestartException private[akka] (actor: ActorRef,
                                                    cause: Throwable, originalCause: Throwable, messageOption: Option[Any])
  extends ActorInitializationException(actor,
    "exception in preRestart(" +
      (if (originalCause == null) "null" else originalCause.getClass) + ", " +
      (messageOption match { case Some(m: AnyRef) ⇒ m.getClass; case _ ⇒ "None" }) +
      ")", cause)

/**
 * A PostRestartException is thrown when constructor or postRestart() method
 * fails during a restart attempt.
 *
 * @param actor is the actor whose constructor or postRestart() hook failed
 * @param cause is the exception thrown by that actor within preRestart()
 * @param originalCause is the exception which caused the restart in the first place
 */
final case class PostRestartException private[akka] (actor: ActorRef,
                                                     cause: Throwable, originalCause: Throwable)
  extends ActorInitializationException(actor,
    "exception post restart (" + (
      if (originalCause == null) "null"
      else originalCause.getClass) + ")", cause)

/**
 * This is an extractor for retrieving the original cause (i.e. the first
 * failure) from a [[org.scalajs.actors.PostRestartException]]. In the face of
 * multiple “nested” restarts it will walk the origCause-links until it arrives
 * at a non-PostRestartException type.
 */
object OriginalRestartException {
  def unapply(ex: PostRestartException): Option[Throwable] = {
    @tailrec def rec(ex: PostRestartException): Option[Throwable] = ex match {
      case PostRestartException(_, _, e: PostRestartException) ⇒ rec(e)
      case PostRestartException(_, _, e)                       ⇒ Some(e)
    }
    rec(ex)
  }
}

/**
 * InvalidMessageException is thrown when an invalid message is sent to an
 * Actor.
 * Currently only `null` is an invalid message.
 */
final case class InvalidMessageException private[akka] (
  message: String) extends AkkaException(message)

/**
 * A DeathPactException is thrown by an Actor that receives a
 * Terminated(someActor) message that it doesn't handle itself, effectively
 * crashing the Actor and escalating to the supervisor.
 */
final case class DeathPactException private[akka] (dead: ActorRef)
  extends AkkaException("Monitored actor [" + dead + "] terminated")

/**
 * This message is published to the EventStream whenever an Actor receives a
 * message it doesn't understand.
 */
final case class UnhandledMessage(message: Any, sender: ActorRef,
                                  recipient: ActorRef)

/**
 * Classes for passing status back to the sender.
 * Used for internal ACKing protocol. But exposed as utility class for
 * user-specific ACKing protocols as well.
 */
object Status {
  sealed trait Status extends Serializable

  /**
   * This class/message type is preferably used to indicate success of some
   * operation performed.
   */
  case class Success(status: AnyRef) extends Status

  /**
   * This class/message type is preferably used to indicate failure of some
   * operation performed.
   * As an example, it is used to signal failure with AskSupport is used (ask/?).
   */
  case class Failure(cause: Throwable) extends Status
}

/**
 * Scala API: Mix in ActorLogging into your Actor to easily obtain a reference to a logger,
 * which is available under the name "log".
 *
 * {{{
 * class MyActor extends Actor with ActorLogging {
 *   def receive = {
 *     case "pigdog" => log.info("We've got yet another pigdog on our hands")
 *   }
 * }
 * }}}
 */
trait ActorLogging { this: Actor ⇒
  //val log = akka.event.Logging(context.system, this)
  object log {
    import akka.event.Logging._
    private def publish(event: LogEvent) = context.system.eventStream.publish(event)
    private def myClass = ActorLogging.this.getClass

    def debug(msg: String): Unit = publish(Debug(self.toString, myClass, msg))
    def info(msg: String): Unit = publish(Info(self.toString, myClass, msg))
    def warning(msg: String): Unit = publish(Warning(self.toString, myClass, msg))
    def error(msg: String): Unit = publish(Error(self.toString, myClass, msg))
  }
}

object Actor {
  /**
   * Type alias representing a Receive-expression for Akka Actors.
   */
  type Receive = PartialFunction[Any, Unit]

  /**
   * emptyBehavior is a Receive-expression that matches no messages at all, ever.
   */
  @SerialVersionUID(1L)
  object emptyBehavior extends Receive {
    def isDefinedAt(x: Any) = false
    def apply(x: Any) = throw new UnsupportedOperationException("Empty behavior apply()")
  }

  /**
   * Default placeholder (null) used for "!" to indicate that there is no sender of the message,
   * that will be translated to the receiving system's deadLetters.
   */
  final val noSender: ActorRef = null
}

trait Actor {

  import Actor._

  // to make type Receive known in subclasses without import
  type Receive = Actor.Receive

  private[this] var _context: ActorContext = {
    val contextStack = ActorCell.contextStack
    if ((contextStack.isEmpty) || (contextStack.head eq null))
      throw ActorInitializationException(
        s"You cannot create an instance of [${getClass.getName}] explicitly using the constructor (new). " +
          "You have to use one of the 'actorOf' factory methods to create a new actor. See the documentation.")
    val c = contextStack.head
    ActorCell.contextStack = null :: contextStack
    c
  }

  private[this] var _self: ActorRef = context.self

  private[akka] final def setActorFields(context: ActorContext,
                                         self: ActorRef): Unit = {
    this._context = context
    this._self = self
  }

  /**
   * Stores the context for this actor, including self, and sender.
   * It is implicit to support operations such as `forward`.
   *
   * WARNING: Only valid within the Actor itself, so do not close over it and
   * publish it to other threads!
   */
  implicit final def context: ActorContext = _context

  /**
   * The 'self' field holds the ActorRef for this actor.
   * <p/>
   * Can be used to send messages to itself:
   * <pre>
   * self ! message
   * </pre>
   */
  implicit final def self: ActorRef = _self

  /**
   * The reference sender Actor of the last received message.
   * Is defined if the message was sent from another Actor,
   * else `deadLetters` in [[akka.actor.ActorSystem]].
   *
   * WARNING: Only valid within the Actor itself, so do not close over it and
   * publish it to other threads!
   */
  final def sender: ActorRef = context.sender

  /**
   * This defines the initial actor behavior, it must return a partial function
   * with the actor logic.
   */
  def receive: Actor.Receive

  /**
   * User overridable definition the strategy to use for supervising
   * child actors.
   */
  def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.defaultStrategy

  // Life-cycle hooks

  /**
   * User overridable callback.
   * <p/>
   * Is called when an Actor is started.
   * Actors are automatically started asynchronously when created.
   * Empty default implementation.
   */
  def preStart(): Unit = ()

  /**
   * User overridable callback.
   * <p/>
   * Is called asynchronously after 'actor.stop()' is invoked.
   * Empty default implementation.
   */
  def postStop(): Unit = ()

  /**
   * User overridable callback: '''By default it disposes of all children and
   * then calls `postStop()`.'''
   * @param reason the Throwable that caused the restart to happen
   * @param message optionally the current message the actor processed when failing, if applicable
   * <p/>
   * Is called on a crashed Actor right BEFORE it is restarted to allow clean
   * up of resources before Actor is terminated.
   */
  def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    context.children foreach { child ⇒
      context.unwatch(child)
      context.stop(child)
    }
    postStop()
  }

  /**
   * User overridable callback: By default it calls `preStart()`.
   * @param reason the Throwable that caused the restart to happen
   * <p/>
   * Is called right AFTER restart on the newly created Actor to allow
   * reinitialization after an Actor crash.
   */
  def postRestart(reason: Throwable): Unit = {
    preStart()
  }

  // Unhandled message

  /**
   * User overridable callback.
   * <p/>
   * Is called when a message isn't handled by the current behavior of the actor
   * by default it fails with either a [[akka.actor.DeathPactException]] (in
   * case of an unhandled [[akka.actor.Terminated]] message) or publishes an [[akka.actor.UnhandledMessage]]
   * to the actor's system's [[akka.event.EventStream]]
   */
  def unhandled(message: Any): Unit = {
    message match {
      case Terminated(dead) ⇒ throw new DeathPactException(dead)
      case _ ⇒
        context.system.eventStream.publish(UnhandledMessage(message, sender, self))
    }
  }
}
