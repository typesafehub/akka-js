package akka.actor

import scala.scalajs.js
import scala.reflect.ClassTag
import scala.collection.immutable

final class Props(clazz: Class[_ <: Actor], creator: () ⇒ Actor) {
  private[akka] def newActor(): Actor =
    creator()
}

object Props {

  def apply[A <: Actor: ClassTag](creator: ⇒ A): Props =
    apply(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[_ <: Actor]],
      () ⇒ creator)

  def apply(clazz: Class[_ <: Actor], creator: () ⇒ Actor): Props =
    new Props(clazz, creator)

  def apply(clazz: Class[_ <: Actor]): Props = {
    val producer = new ArgsReflectConstructor(clazz, immutable.Seq.empty[Any])
    new Props(clazz, () ⇒ producer.produce())
  }

}

/**
 * This interface defines a class of actor creation strategies deviating from
 * the usual default of just reflectively instantiating the [[Actor]]
 * subclass. It can be used to allow a dependency injection framework to
 * determine the actual actor class and how it shall be instantiated.
 */
trait IndirectActorProducer {

  /**
   * This factory method must produce a fresh actor instance upon each
   * invocation. <b>It is not permitted to return the same instance more than
   * once.</b>
   */
  def produce(): Actor

  /**
   * This method is used by [[Props]] to determine the type of actor which will
   * be created. This means that an instance of this `IndirectActorProducer`
   * will be created in order to call this method during any call to
   * [[Props#actorClass]]; it should be noted that such calls may
   * performed during actor set-up before the actual actor’s instantiation, and
   * that the instance created for calling `actorClass` is not necessarily reused
   * later to produce the actor.
   */
  def actorClass: Class[_ <: Actor]
}

/**
 * INTERNAL API
 */
private[akka] class ArgsReflectConstructor(clz: Class[_ <: Actor], args: immutable.Seq[Any]) extends IndirectActorProducer {
  override def actorClass = clz
  override def produce() = { //Reflect.instantiate(constructor, args).asInstanceOf[Actor]
    val fqcn = clz.getName
    // assumes `fqcn` has just one ctor
    val splitName = fqcn.split("\\.")

    val constructor = (js.Dynamic.global /: splitName) { (prev, part) ⇒
      prev.selectDynamic(part)
    }

    val castArgs = args.map(_.asInstanceOf[js.Any]) // they will go back to Scala, so that's always fine
    val obj = js.Dynamic.newInstance(constructor)(castArgs: _*)
    obj.asInstanceOf[Actor]
  }
}
