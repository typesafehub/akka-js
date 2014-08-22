package akkajs.test

import scala.util.{Try, Success, Failure}
import scala.concurrent.Promise
import akka.actor.{Actor, ActorRef, Props, ActorSystem, ActorInitializationException}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue


class ActorRefTestSuite(system: ActorSystem) extends TestSuite with AsyncAssert {

  /**
   *  If the evaluation of `f` throws an exception `e`, invoking
   *  `promiseIntercept` throws `e`, too. In addition, promise `to`
   *  is completed with `e`.
   */
  def promiseIntercept(f: ⇒ Actor)(to: Promise[Actor]): Actor = try {
    val r = f
    to.success(r)
    r
  } catch {
    case e: Throwable ⇒
      to.failure(e)
      throw e
  }

  def wrapAsync[T](f: Promise[Actor] ⇒ T)(cont: Try[T] => Unit): Unit = {
    val result = Promise[Actor]()
    val r = Try(f(result))
    // TODO: invoke cont(Failure(t)) after a timeout of like 1 minute
    result.future.map(v => cont(r)).recover {
      case t: Throwable => cont(Failure(t))
    }
  }

  def numTests: Int = 2

  def testMain(): Unit = {
    intercept[akka.actor.ActorInitializationException] {
      new Actor { def receive = { case _ ⇒ } }
    }

    wrapAsync[ActorRef]({ result ⇒
      system.actorOf(Props(new Actor {
        val nested = promiseIntercept(new Actor { def receive = { case _ ⇒ } })(result)
        def receive = { case _ ⇒ }
      }))
    }) { outcome => checkIntercept[ActorInitializationException, ActorRef](outcome) }
  }

}
