package akka.actor

import scala.annotation.tailrec

/** Base class for actors exceptions. */
class ActorsException(message: String,
    cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}
