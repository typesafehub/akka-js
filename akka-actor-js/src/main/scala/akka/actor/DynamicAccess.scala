/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.actor

import scala.reflect.{ ClassTag, classTag }

import scala.collection.immutable
import scala.util.Try
import scala.scalajs.js

/**
 * The DynamicAccess implementation is the class which is used for
 * loading all configurable parts of an actor system (the
 * [[akka.actor.ReflectiveDynamicAccess]] is the default implementation).
 *
 * This is an internal facility and users are not expected to encounter it
 * unless they are extending Akka in ways which go beyond simple Extensions.
 */
abstract class DynamicAccess {
  /**
   * Convenience method which given a `Class[_]` object and a constructor description
   * will create a new instance of that class.
   *
   * {{{
   * val obj = DynamicAccess.createInstanceFor(clazz, Seq(classOf[Config] -> config, classOf[String] -> name))
   * }}}
   */
  def createInstanceFor[T: ClassTag](clazz: Class[_], args: immutable.Seq[(Class[_], AnyRef)]): Try[T]

  /**
   * Obtain an object conforming to the type T, which is expected to be
   * instantiated from a class designated by the fully-qualified class name
   * given, where the constructor is selected and invoked according to the
   * `args` argument. The exact usage of args depends on which type is requested,
   * see the relevant requesting code for details.
   */
  def createInstanceFor[T: ClassTag](fqcn: String, args: immutable.Seq[(Class[_], AnyRef)]): Try[T]

  /**
   * Obtain the Scala “object” instance for the given fully-qualified class name, if there is one.
   */
  def getObjectFor[T: ClassTag](fqcn: String): Try[T]
}

class JSDynamicAccess extends DynamicAccess {

  def createInstanceFor[T: ClassTag](clazz: Class[_], args: immutable.Seq[(Class[_], AnyRef)]): Try[T] =
    createInstanceFor[T](clazz.getSimpleName(), args)

  def createInstanceFor[T: ClassTag](fqcn: String, args: immutable.Seq[(Class[_], AnyRef)]): Try[T] = Try {
    // assumes `fqcn` has just one ctor
    val splitName = fqcn.split("\\.")

    val constructor = (js.Dynamic.global /: splitName) { (prev, part) ⇒
      prev.selectDynamic(part)
    }

    val castArgs = args.map { case (c, v) ⇒ v.asInstanceOf[js.Any] } // they will go back to Scala, so that's always fine
    val obj = js.Dynamic.newInstance(constructor)(castArgs: _*)

    val t = classTag[T].runtimeClass
    if (t.isInstance(obj)) obj.asInstanceOf[T] else throw new ClassCastException(fqcn + " is not a subtype of " + t)
  }

  def getObjectFor[T: ClassTag](fqcn: String): Try[T] = {
    // skip trailing '$' of `fqcn` (if any)
    val fullName = if (fqcn.endsWith("$")) fqcn.init else fqcn
    val splitName = fullName.split("\\.")

    Try {
      val prefix = (js.Dynamic.global /: splitName.init) { (prev, part) ⇒
        prev.selectDynamic(part)
      }

      // the name of the object has to be selected using `applyDynamic`
      val obj = prefix.applyDynamic(splitName.last)()

      val t = classTag[T].runtimeClass
      obj match {
        case null                  ⇒ throw new NullPointerException
        case x if !t.isInstance(x) ⇒ throw new ClassCastException(fqcn + " is not a subtype of " + t)
        case x: T                  ⇒ x
      }
    }
  }
}
