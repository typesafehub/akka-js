package akka.scalajs.util

import org.scalajs.dom

import scala.concurrent.duration.FiniteDuration

import akka.actor.Cancellable

class JSTimeoutTask(delay: FiniteDuration, task: ⇒ Any) extends Cancellable {
  private[this] var underlying: Option[Int] =
    Some(dom.setTimeout(() ⇒ task, delay.toMillis))

  def isCancelled: Boolean = underlying.isEmpty

  def cancel(): Boolean = {
    if (isCancelled) false
    else {
      dom.clearTimeout(underlying.get)
      underlying = None
      true
    }
  }
}

object JSTimeoutTask {
  def apply(duration: FiniteDuration)(task: ⇒ Any): JSTimeoutTask =
    new JSTimeoutTask(duration, task)
}
