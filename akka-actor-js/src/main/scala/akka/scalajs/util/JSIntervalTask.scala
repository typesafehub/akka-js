package akka.scalajs.util

import org.scalajs.dom

import scala.concurrent.duration.FiniteDuration

import akka.actor.Cancellable

class JSIntervalTask(interval: FiniteDuration, task: ⇒ Any) extends Cancellable {
  private[this] var underlying: Option[Int] =
    Some(dom.setInterval(() ⇒ task, interval.toMillis))

  def isCancelled: Boolean = underlying.isEmpty

  def cancel(): Boolean = {
    if (isCancelled) false
    else {
      dom.clearInterval(underlying.get)
      underlying = None
      true
    }
  }
}

object JSIntervalTask {
  def apply(interval: FiniteDuration)(task: ⇒ Any): JSIntervalTask =
    new JSIntervalTask(interval, task)
}
