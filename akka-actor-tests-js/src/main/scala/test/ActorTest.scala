package test

import scala.scalajs.js
import js.Dynamic.global

import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor._
import akka.pattern.Ask._
import akka.util.Timeout


case class Greeting(who: String)
case class Request(x: Int)

trait Util {
  def appendText(text: String): Unit = {
    val paragraph = dom.document.createElement("p")
    paragraph.innerHTML = s"<strong>$text</strong>"
    dom.document.getElementById("playground").appendChild(paragraph)
  }
}

class GreetingActor extends Actor with Util {
  def receive = {
    case Greeting(who) =>
      global.console.log("Hello " + who)
      appendText("hello there again, kiddo")

    case Request(y) =>
      sender ! s"$self's response: $y"
  }
}

object ActorTest extends js.JSApp with Util {

  def main(): Unit = {
    appendText("hello there, kiddo")
    global.console.log("Starting test")

    val system = ActorSystem("MySystem")
    global.console.log("Actor system created")

    val greeter = system.actorOf(Props(new GreetingActor), name = "greeter")
    global.console.log("Actor created")
    greeter ! Greeting("Charlie Parker")

    implicit val timeout: Timeout = 2.seconds
    (greeter ? Request(5)) map { res =>
      appendText(s"received response: $res")
    }
  }

}
