package akka.actor

import scala.concurrent.Future

import akka.dispatch._
import akka.event._
import scala.collection.mutable.HashMap

object ActorSystem {

  /**
   * Creates a new ActorSystem with the name "default",
   * obtains the current ClassLoader by first inspecting the current threads' getContextClassLoader,
   * then tries to walk the stack to find the callers class loader, then falls back to the ClassLoader
   * associated with the ActorSystem class.
   * Then it loads the default reference configuration using the ClassLoader.
   */
  def apply(): ActorSystem = apply("default")

  /**
   * Creates a new ActorSystem with the specified name,
   * obtains the current ClassLoader by first inspecting the current threads' getContextClassLoader,
   * then tries to walk the stack to find the callers class loader, then falls back to the ClassLoader
   * associated with the ActorSystem class.
   * Then it loads the default reference configuration using the ClassLoader.
   */
  def apply(name: String): ActorSystem = {
    new ActorSystemImpl(name).start()
  }

  /**
   * Settings are the overall ActorSystem Settings which also provides a convenient access to the Config object.
   *
   * For more detailed information about the different possible configuration options, look in the Akka Documentation under "Configuration"
   *
   * @see <a href="http://typesafehub.github.com/config/v0.4.1/" target="_blank">The Typesafe Config Library API Documentation</a>
   */
  class Settings( final val name: String) {

    final val LogDeadLetters: Int = 0
    final val LogDeadLettersDuringShutdown: Boolean = false

    final val AddLoggingReceive: Boolean = true
    final val DebugAutoReceive: Boolean = false
    final val DebugLifecycle: Boolean = true
    final val DebugEventStream: Boolean = false
    final val DebugUnhandledMessage: Boolean = false

    override def toString: String = s"Settings($name)"

  }
}

abstract class ActorSystem extends ActorRefFactory {
  import ActorSystem._

  /**
   * The name of this actor system, used to distinguish multiple ones within
   * the same JVM & class loader.
   */
  def name: String

  /**
   * The core settings extracted from the supplied configuration.
   */
  def settings: Settings

  def scheduler: Scheduler
  def eventStream: EventStream
  def provider: ActorRefProvider

  def deadLetters: ActorRef

  def shutdown(): Unit

  def sendToPath(path: ActorPath, message: Any)(
    implicit sender: ActorRef = Actor.noSender): Unit

  /**
   * Registers the provided extension and creates its payload, if this extension isn't already registered
   * This method has putIfAbsent-semantics, this method can potentially block, waiting for the initialization
   * of the payload, if is in the process of registration from another Thread of execution
   */
  def registerExtension[T <: Extension](ext: ExtensionId[T]): T

  /**
   * Returns the payload that is associated with the provided extension
   * throws an IllegalStateException if it is not registered.
   * This method can potentially block, waiting for the initialization
   * of the payload, if is in the process of registration from another Thread of execution
   */
  def extension[T <: Extension](ext: ExtensionId[T]): T

  /**
   * Returns whether the specified extension is already registered, this method can potentially block, waiting for the initialization
   * of the payload, if is in the process of registration from another Thread of execution
   */
  def hasExtension(ext: ExtensionId[_ <: Extension]): Boolean
}

/**
 * More powerful interface to the actor system’s implementation which is presented to extensions (see [[akka.actor.Extension]]).
 *
 * <b><i>Important Notice:</i></b>
 *
 * This class is not meant to be extended by user code. If you want to
 * actually roll your own Akka, beware that you are completely on your own in
 * that case!
 */
abstract class ExtendedActorSystem extends ActorSystem {

  /**
   * The ActorRefProvider is the only entity which creates all actor references within this actor system.
   */
  def provider: ActorRefProvider

  /**
   * The top-level supervisor of all actors created using system.actorOf(...).
   */
  def guardian: InternalActorRef

  /**
   * The top-level supervisor of all system-internal services like logging.
   */
  def systemGuardian: InternalActorRef

  /**
   * Create an actor in the "/system" namespace. This actor will be shut down
   * during system shutdown only after all user actors have terminated.
   */
  def systemActorOf(props: Props, name: String): ActorRef

  /**
   * A ThreadFactory that can be used if the transport needs to create any Threads
   */
  // def threadFactory: ThreadFactory

  /**
   * ClassLoader wrapper which is used for reflective accesses internally. This is set
   * to use the context class loader, if one is set, or the class loader which
   * loaded the ActorSystem implementation. The context class loader is also
   * set on all threads created by the ActorSystem, if one was set during
   * creation.
   */
  def dynamicAccess: DynamicAccess

  /**
   * For debugging: traverse actor hierarchy and make string representation.
   * Careful, this may OOM on large actor systems, and it is only meant for
   * helping debugging in case something already went terminally wrong.
   */
  private[akka] def printTree: String
}

class ActorSystemImpl(val name: String) extends ExtendedActorSystem
  with akka.scalajs.webworkers.WebWorkersActorSystem {

  import ActorSystem._

  final val settings: Settings = new Settings(name)

  protected def systemImpl = this

  def actorOf(props: Props): ActorRef =
    guardian.actorCell.actorOf(props)
  def actorOf(props: Props, name: String): ActorRef =
    guardian.actorCell.actorOf(props, name)

  def stop(actor: ActorRef): Unit = {
    val path = actor.path
    val guard = guardian.path
    val sys = systemGuardian.path
    path.parent match {
      case `guard` ⇒ guardian ! StopChild(actor)
      case `sys`   ⇒ systemGuardian ! StopChild(actor)
      case _       ⇒ actor.asInstanceOf[InternalActorRef].stop()
    }
  }

  val scheduler: Scheduler = new EventLoopScheduler
  val eventStream: EventStream = new EventStream
  val provider: ActorRefProvider = new LocalActorRefProvider(
    name, settings, eventStream)

  def deadLetters: ActorRef = provider.deadLetters

  val mailboxes: Mailboxes = new Mailboxes(deadLetters)
  val dispatcher: MessageDispatcher = new MessageDispatcher(mailboxes)

  def terminationFuture: Future[Unit] = provider.terminationFuture
  def lookupRoot: InternalActorRef = provider.rootGuardian
  def guardian: LocalActorRef = provider.guardian
  def systemGuardian: LocalActorRef = provider.systemGuardian

  def /(actorName: String): ActorPath = guardian.path / actorName
  def /(path: Iterable[String]): ActorPath = guardian.path / path

  private lazy val _start: this.type = {
    // the provider is expected to start default loggers, LocalActorRefProvider does this
    provider.init(this)
    //if (settings.LogDeadLetters > 0)
    //  logDeadLetterListener = Some(systemActorOf(Props[DeadLetterListener], "deadLetterListener"))
    //registerOnTermination(stopScheduler())
    //loadExtensions()
    //if (LogConfigOnStart) logConfiguration()
    this
  }

  def start(): this.type = _start

  def shutdown(): Unit = {
    guardian.stop()
  }

  override def sendToPath(path: ActorPath, message: Any)(
    implicit sender: ActorRef): Unit = {
    // FIXME The existence of this method is a hack! Need to find a solution.
    new akka.scalajs.webworkers.WorkerActorRef(this, path) ! message
  }

  def resolveLocalActorPath(path: ActorPath): Option[ActorRef] = {
    println(path.elements)
    //Some(provider.resolveActorRef(provider.rootPath / path.elements))
    val result = path.elements.tail.foldLeft(provider.guardian) { (parent, childName) ⇒
      parent.actorCell.child(childName) match {
        case Some(child: LocalActorRef) ⇒ child
        case x ⇒
          println(s"$parent of name ${parent.path}.child($childName) = $x")
          return None
      }
    }
    Some(result)
  }

  def dynamicAccess: DynamicAccess = new JSDynamicAccess

  private[akka] def printTree: String = ???

  private val extensions = new HashMap[ExtensionId[_], AnyRef]

  def systemActorOf(props: Props, name: String): ActorRef = ???

  /**
   * Returns any extension registered to the specified Extension or returns null if not registered
   */
  private def findExtension[T <: Extension](ext: ExtensionId[T]): T = extensions.get(ext).orNull.asInstanceOf[T]

  def registerExtension[T <: akka.actor.Extension](ext: akka.actor.ExtensionId[T]): T = {
    findExtension(ext) match {
      case null ⇒ //Doesn't already exist, commence registration
        ext.createExtension(this) match { // Create and initialize the extension
          case null ⇒ throw new IllegalStateException("Extension instance created as 'null' for extension [" + ext + "]")
          case instance ⇒
            extensions += (ext -> instance)
            instance //Profit!
        }
      case existing ⇒ existing.asInstanceOf[T]
    }
  }

  def extension[T <: akka.actor.Extension](ext: akka.actor.ExtensionId[T]): T = findExtension(ext) match {
    case null ⇒ throw new IllegalArgumentException("Trying to get non-registered extension [" + ext + "]")
    case some ⇒ some.asInstanceOf[T]
  }

  def hasExtension(ext: akka.actor.ExtensionId[_ <: akka.actor.Extension]): Boolean = findExtension(ext) != null
}
