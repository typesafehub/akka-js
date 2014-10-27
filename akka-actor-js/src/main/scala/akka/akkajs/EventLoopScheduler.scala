package akka.akkajs

import akka.scalajs.util.{ JSTimeoutThenIntervalTask, JSTimeoutTask }

import akka.actor.{ Scheduler, Cancellable }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class EventLoopScheduler extends Scheduler {

  def schedule(initialDelay: FiniteDuration, interval: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable = {
    JSTimeoutThenIntervalTask(initialDelay, interval)(f)
  }

  def scheduleOnce(delay: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable = {
    JSTimeoutTask(delay)(f)
  }

  def maxFrequency: Double = {
    // as per HTML spec
    1.0 / 0.0004
  }

}
