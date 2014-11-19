package akka.actor

import scala.language.implicitConversions

import scala.scalajs.js

import akka.dispatch._
import akka.dispatch.sysmsg._
import akka.event.EventStream

/**
 * Immutable and serializable handle to an actor, which may or may not reside
 * on the local host or inside the same [[org.scalajs.actors.ActorSystem]]. An ActorRef
 * can be obtained from an [[org.scalajs.actors.ActorRefFactory]], an interface which
 * is implemented by ActorSystem and [[org.scalajs.actors.ActorContext]]. This means
 * actors can be created top-level in the ActorSystem or as children of an
 * existing actor, but only from within that actor.
 *
 * ActorRefs can be freely shared among actors by message passing. Message
 * passing conversely is their only purpose.
 *
 * ActorRef does not have a method for terminating the actor it points to, use
 * [[akka.actor.ActorRefFactory]]`.stop(ref)`, or send a [[akka.actor.PoisonPill]],
 * for this purpose.
 *
 * Two actor references are compared equal when they have the same path and point to
 * the same actor incarnation. A reference pointing to a terminated actor doesn't compare
 * equal to a reference pointing to another (re-created) actor with the same path.
 * Actor references acquired with `actorFor` do not always include the full information
 * about the underlying actor identity and therefore such references do not always compare
 * equal to references acquired with `actorOf`, `sender`, or `context.self`.
 *
 * If you need to keep track of actor references in a collection and do not care
 * about the exact actor incarnation you can use the ``ActorPath`` as key because
 * the unique id of the actor is not taken into account when comparing actor paths.
 */
abstract class ActorRef { internalRef: InternalActorRef ⇒

  def path: ActorPath

  def tell(message: Any, sender: ActorRef): Unit = this.!(message)(sender)

  /**
   * Forwards the message and passes the original sender actor as the sender.
   *
   * Works with '!' and '?'/'ask'.
   */
  def forward(message: Any)(implicit context: ActorContext): Unit = tell(message, context.sender)

  /**
   * Is the actor shut down?
   * The contract is that if this method returns true, then it will never be false again.
   * But you cannot rely on that it is alive if it returns false, since this by nature is a racy method.
   */
  @deprecated("Use context.watch(actor) and receive Terminated(actor)", "2.2") def isTerminated: Boolean

  /**
   * Comparison takes path and the unique id of the actor cell into account.
   */
  final def compareTo(other: ActorRef) = {
    val x = this.path compareTo other.path
    if (x == 0) {
      if (this.path.uid < other.path.uid) -1
      else if (this.path.uid == other.path.uid) 0
      else 1
    } else x
  }

  final override def hashCode: Int = {
    if (path.uid == ActorCell.undefinedUid) path.hashCode
    else path.uid
  }

  /**
   * Equals takes path and the unique id of the actor cell into account.
   */
  final override def equals(that: Any): Boolean = that match {
    case other: ActorRef ⇒ path.uid == other.path.uid && path == other.path
    case _               ⇒ false
  }

  override def toString: String =
    if (path.uid == ActorCell.undefinedUid) s"Actor[${path}]"
    else s"Actor[${path}#${path.uid}]"
}

/**
 * This trait represents the Scala Actor API
 * There are implicit conversions in ../actor/Implicits.scala
 * from ActorRef -> ScalaActorRef and back
 */
trait ScalaActorRef { ref: ActorRef ⇒

  /**
   * Sends a one-way asynchronous message. E.g. fire-and-forget semantics.
   * <p/>
   *
   * If invoked from within an actor then the actor reference is implicitly passed on as the implicit 'sender' argument.
   * <p/>
   *
   * This actor 'sender' reference is then available in the receiving actor in the 'sender()' member variable,
   * if invoked from within an Actor. If not then no sender is available.
   * <pre>
   *   actor ! message
   * </pre>
   * <p/>
   */
  def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit

}

/**
 * All ActorRefs have a scope which describes where they live. Since it is
 * often necessary to distinguish between local and non-local references, this
 * is the only method provided on the scope.
 */
private[akka] trait ActorRefScope {
  def isLocal: Boolean
}

/**
 * Internal trait for assembling all the functionality needed internally on
 * ActorRefs. NOTE THAT THIS IS NOT A STABLE EXTERNAL INTERFACE!
 */
private[akka] abstract class InternalActorRef extends ActorRef with ScalaActorRef {
  /*
   * Actor life-cycle management, invoked only internally (in response to user requests via ActorContext).
   */
  def start(): Unit
  def resume(causedByFailure: Throwable): Unit
  def suspend(): Unit
  def restart(cause: Throwable): Unit
  def stop(): Unit
  def sendSystemMessage(message: SystemMessage): Unit

  /**
   * Get a reference to the actor ref provider which created this ref.
   */
  def provider: ActorRefProvider

  /**
   * Obtain parent of this ref; used by getChild for ".." paths.
   */
  def getParent: InternalActorRef

  /**
   * Obtain ActorRef by possibly traversing the actor tree or looking it up at
   * some provider-specific location. This method shall return the end result,
   * i.e. not only the next step in the look-up; this will typically involve
   * recursive invocation. A path element of ".." signifies the parent, a
   * trailing "" element must be disregarded. If the requested path does not
   * exist, return Nobody.
   */
  def getChild(name: Iterator[String]): InternalActorRef

  /**
   * Scope: if this ref points to an actor which resides within the same
   * JS VM, i.e., whose mailbox is directly reachable etc.
   */
  def isLocal: Boolean
}

private[akka] object InternalActorRef {
  // TODO Not sure this is a good idea ...
  implicit def fromActorRef(ref: ActorRef): InternalActorRef =
    ref.asInstanceOf[InternalActorRef]
}

/**
 * Trait for ActorRef implementations where all methods contain default stubs.
 *
 * INTERNAL API
 */
private[akka] trait MinimalActorRef extends InternalActorRef {

  override def start(): Unit = ()
  override def suspend(): Unit = ()
  override def resume(causedByFailure: Throwable): Unit = ()
  override def stop(): Unit = ()
  @deprecated("Use context.watch(actor) and receive Terminated(actor)", "2.2") override def isTerminated = false

  override def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit = ()

  override def sendSystemMessage(message: SystemMessage): Unit = ()
  override def restart(cause: Throwable): Unit = ()

  override def getParent: InternalActorRef = Nobody
  override def getChild(names: Iterator[String]): InternalActorRef =
    if (names.forall(_.isEmpty)) this else Nobody

  override def isLocal: Boolean = true
}

/**
 * When a message is sent to an Actor that is terminated before receiving the
 * message, it will be sent as a DeadLetter to the ActorSystem's EventStream
 */
final case class DeadLetter(message: Any, sender: ActorRef,
                            recipient: ActorRef) {
  require(sender ne null, "DeadLetter sender may not be null")
  require(recipient ne null, "DeadLetter recipient may not be null")
}

/**
 * This special dead letter reference has a name: it is that which is returned
 * by a local look-up which is unsuccessful.
 *
 * INTERNAL API
 */
private[akka] class EmptyLocalActorRef(
  override val provider: ActorRefProvider,
  override val path: ActorPath,
  val eventStream: EventStream) extends MinimalActorRef {

  @deprecated("Use context.watch(actor) and receive Terminated(actor)", "2.2") override def isTerminated = true

  override def sendSystemMessage(message: SystemMessage): Unit = {
    //if (Mailbox.debug) println(s"ELAR $path having enqueued $message")
    specialHandle(message, provider.deadLetters)
  }

  override def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit = message match {
    case null ⇒ throw new InvalidMessageException("Message is null")
    case d: DeadLetter ⇒
      specialHandle(d.message, d.sender) // do NOT form endless loops, since deadLetters will resend!
    case _ if !specialHandle(message, sender) ⇒
    //eventStream.publish(DeadLetter(message, if (sender eq Actor.noSender) provider.deadLetters else sender, this))
    case _                                    ⇒
  }

  protected def specialHandle(msg: Any, sender: ActorRef): Boolean = msg match {
    case w: Watch ⇒
      if (w.watchee == this && w.watcher != this)
        w.watcher.sendSystemMessage(
          DeathWatchNotification(w.watchee, existenceConfirmed = false, addressTerminated = false))
      true
    case _: Unwatch ⇒ true // Just ignore
    case Identify(messageId) ⇒
      sender ! ActorIdentity(messageId, None)
      true
    /*case s: SelectChildName =>
      s.identifyRequest match {
        case Some(identify) => sender ! ActorIdentity(identify.messageId, None)
        case None =>
          //eventStream.publish(DeadLetter(s.wrappedMessage, if (sender eq Actor.noSender) provider.deadLetters else sender, this))
      }
      true*/
    case _ ⇒ false
  }
}

/**
 * Internal implementation of the dead letter destination: will publish any
 * received message to the eventStream, wrapped as [[akka.actor.DeadLetter]].
 *
 * INTERNAL API
 */
private[akka] class DeadLetterActorRef(_provider: ActorRefProvider,
                                       _path: ActorPath, _eventStream: EventStream)
  extends EmptyLocalActorRef(_provider, _path, _eventStream) {

  override def !(message: Any)(implicit sender: ActorRef = this): Unit = message match {
    case null                ⇒ throw new InvalidMessageException("Message is null")
    case Identify(messageId) ⇒ sender ! ActorIdentity(messageId, Some(this))
    case d: DeadLetter ⇒
      if (!specialHandle(d.message, d.sender))
        () //eventStream.publish(d)
    case _ ⇒
      if (!specialHandle(message, sender))
        () //eventStream.publish(DeadLetter(message, if (sender eq Actor.noSender) provider.deadLetters else sender, this))
  }

  override protected def specialHandle(msg: Any, sender: ActorRef): Boolean = msg match {
    case w: Watch ⇒
      if (w.watchee != this && w.watcher != this)
        w.watcher.sendSystemMessage(DeathWatchNotification(w.watchee,
          existenceConfirmed = false, addressTerminated = false))
      true
    case _ ⇒ super.specialHandle(msg, sender)
  }
}

/**
 * This is an internal look-up failure token, not useful for anything else.
 */
private[akka] case object Nobody extends MinimalActorRef {
  override val path: RootActorPath =
    RootActorPath(Address("actors", "all-systems"), "/Nobody")

  override def provider =
    throw new UnsupportedOperationException("Nobody does not provide")
}

private[akka] class LocalActorRef(
  system: ActorSystem,
  val path: ActorPath,
  _parent: ActorRef,
  _props: Props,
  _dispatcher: MessageDispatcher) extends InternalActorRef {

  assert(path.uid != ActorCell.undefinedUid || path.isInstanceOf[RootActorPath],
    s"Trying to create a LocalActorRef with uid-less path $path")

  val actorCell: ActorCell =
    new ActorCell(system, _props, _dispatcher, this, _parent)
  actorCell.init(sendSupervise = _parent ne null)

  private[akka] def underlying = actorCell

  /**
   * Is the actor terminated?
   * If this method returns true, it will never return false again, but if it
   * returns false, you cannot be sure if it's alive still (race condition)
   */
  @deprecated("Use context.watch(actor) and receive Terminated(actor)", "2.2") override def isTerminated: Boolean = actorCell.isTerminated

  def !(msg: Any)(implicit sender: ActorRef = Actor.noSender): Unit = actorCell.sendMessage(Envelope(msg, sender, system))

  // InternalActorRef API

  def start(): Unit = actorCell.start()
  def resume(causedByFailure: Throwable): Unit = actorCell.resume(causedByFailure)
  def suspend(): Unit = actorCell.suspend()
  def restart(cause: Throwable): Unit = actorCell.restart(cause)
  def stop(): Unit = actorCell.stop()
  def sendSystemMessage(message: SystemMessage): Unit = actorCell.sendSystemMessage(message)

  def getParent: InternalActorRef = _parent

  override def provider: ActorRefProvider = actorCell.provider

  /**
   * Method for looking up a single child beneath this actor. Override in order
   * to inject “synthetic” actor paths like “/temp”.
   * It is racy if called from the outside.
   */
  def getSingleChild(name: String): InternalActorRef = {
    val (childName, uid) = ActorCell.splitNameAndUid(name)
    actorCell.childStatsByName(childName) match {
      case Some(crs: ChildRestartStats) if uid == ActorCell.undefinedUid || uid == crs.uid ⇒
        crs.child.asInstanceOf[InternalActorRef]
      case _ ⇒ Nobody
    }
  }

  def getChild(name: Iterator[String]): InternalActorRef =
    Actor.noSender

  def isLocal: Boolean = true
}
