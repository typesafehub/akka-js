package akkajs.test

import scala.scalajs.js
import scala.scalajs.js.Dynamic.global
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.concurrent.duration._

import akka.actor.{ Actor, ActorSystem, ActorRef, Props }
import akka.pattern.Ask._
import akka.util.Timeout


abstract class TestMessage
case class OneWay(message: String) extends TestMessage
case class TwoWay(message: String) extends TestMessage
case class ForwardTo(other: ActorRef) extends TestMessage
case class Response(message: String) extends TestMessage

class SimpleActor extends Actor with AsyncAssert {
  implicit val timeout: Timeout = 2.seconds

  def receive = {
    case OneWay(msg) =>
      test("One-way message receive") { implicit desc: TestDesc =>
        assert1(msg == "hello", s"expected: 'hello', received: '$msg'")
      }

    case TwoWay(msg) =>
      sender ! Response(s"$msg$msg")

    case ForwardTo(other) =>
      val response = other ? TwoWay("hello")
      response.map {
        case Response(msg) =>
          test("Ask pattern") { implicit desc: TestDesc =>
            assert1(msg == "hellohello", s"expected: 'hellohello', received: '$msg'")
          }
      }
  }
}


class ActorTestSuite(system: ActorSystem) extends TestSuite {

  def numTests: Int = 2

  def testMain(): Unit = {
    val simple = system.actorOf(Props(new SimpleActor), "simple")
    simple ! OneWay("hello")

    val consumer = system.actorOf(Props(new SimpleActor), "consumer")
    simple ! ForwardTo(consumer)
  }

}
