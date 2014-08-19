package test

import scala.scalajs.js
import js.Dynamic.global
import org.scalajs.dom

import akka.actor._

case class Greeting(who: String)

class GreetingActor extends Actor {
  def receive = {
    case Greeting(who) =>
      global.console.log("Hello " + who)
      val paragraph = dom.document.createElement("p")
      val result = "hello there, kiddo2"
      paragraph.innerHTML = s"<strong>$result</strong>"
      dom.document.getElementById("playground").appendChild(paragraph)

  }
}

object ActorTest extends js.JSApp {
  def main(): Unit = {
    val paragraph = dom.document.createElement("p")
    val result = "hello there, kiddo"
    paragraph.innerHTML = s"<strong>$result</strong>"
    dom.document.getElementById("playground").appendChild(paragraph)
    global.console.log("Starting test")
    val system = ActorSystem("MySystem")
    global.console.log("Actor system created")
    val greeter = system.actorOf(Props(new GreetingActor), name = "greeter")
    global.console.log("Actor created")
    greeter ! Greeting("Charlie Parker")
  }

  def m(): Unit = {
    error("boom!") // `Predef.error` is deprecated
  }
}
