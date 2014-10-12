package client

import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.dom
import org.scalajs.jquery.{jQuery => jQ, _}

import scala.concurrent.duration._

import akka.actor._
import akka.event.LoggingReceive

import akka.scalajs.jsapi.Timers
import akka.scalajs.wsclient._

import models._


@JSExport("Client")
object Main {
  RegisterPicklers.registerPicklers()

  val notifications = jQ("#notifications")

  val system = ActorSystem("client-system")
  val manager = system.actorOf(Props(new Manager), name = "manager")
  var me: User = User.Nobody

  @JSExport
  def startup(): Unit = {
    jQ("#connect-button") click {
      (event: JQueryEventObject) =>
        connect()
        false
    }
    jQ("#toggle-button") click {
      (event: JQueryEventObject) =>
        manager ! ToggleUpdates(me)
        false
    }
    println("starting up...")
  }

  def connect(): Unit = {
    val name = jQ("#name-edit").value().toString()
    println(s"connecting with name $name...")
    me = User(name, "")
    manager ! AttemptToConnect
  }
}

case object AttemptToConnect
case class Send(text: String)
case object Disconnect
case object Disconnected

class Manager extends Actor {

  // manages the WebSocket client proxy
  val proxyManager = context.actorOf(Props(new ProxyManager))

  private[this] var myStatusAlert: JQuery = jQ()
  def statusAlert: JQuery = myStatusAlert
  def statusAlert_=(v: JQuery): Unit = {
    myStatusAlert.remove()
    myStatusAlert = v
    myStatusAlert.prependTo(Main.notifications)
  }

  def makeStatusAlert(style: String): JQuery =
    jQ(s"""<div class="alert alert-$style">""")

  def receive = disconnected()

  def disconnected(nextReconnectTimeout: FiniteDuration = 1.seconds): Receive = LoggingReceive {
    case m @ AttemptToConnect =>
      proxyManager ! m
      statusAlert = makeStatusAlert("info").append(
        jQ("<strong>Connecting ...</strong>"),
        jQ("<span> </span>").append(
          jQ("""<a href="#">""").text("Cancel").click {
            (e: JQueryEventObject) =>
              self ! Disconnect
              false
          }
        )
      )
      context.setReceiveTimeout(5.seconds)
      context.become(connecting(nextReconnectTimeout))
  }

  def waitingToAutoReconnect(autoReconnectDeadline: FiniteDuration, nextReconnectTimeout: FiniteDuration): Receive = LoggingReceive {
    case m @ AttemptToConnect =>
      context.become(disconnected(nextReconnectTimeout))
      self ! m

    case m @ ReceiveTimeout =>
      val now = nowDuration
      if (now >= autoReconnectDeadline)
        self ! AttemptToConnect
      else {
        val remaining = autoReconnectDeadline - nowDuration
        jQ(".reconnect-remaining-seconds").text(remaining.toSeconds.toString)
      }
  }

  def connecting(nextReconnectTimeout: FiniteDuration): Receive = LoggingReceive { withDisconnected(nextReconnectTimeout) {
    case ReceiveTimeout | Disconnect =>
      context.setReceiveTimeout(Duration.Undefined)
      proxyManager ! Disconnect

    case m @ WebSocketConnected(entryPoint) =>
      println("established a WebSocket connection!!")
      context.setReceiveTimeout(Duration.Undefined)

      val service = entryPoint
      service ! Connect(Main.me)
      val alert = makeStatusAlert("success").append(
        jQ("<strong>").text(s"Connected as ${Main.me.nick}")
      )
      statusAlert = alert
      Timers.setTimeout(3000) {
        alert.fadeOut()
      }

      context.become(connected(service))
  } }

  var currentSender: ActorRef = _

  def connected(service: ActorRef): Receive = LoggingReceive { withDisconnected() {
    case m @ Message(user, text, timestamp) =>
      currentSender = sender
      MessagesContainer.addMessage(m)

    case m @ ToggleUpdates(user) =>
      println(s"client manager, toggle updates for $user")
      currentSender ! m

    case m @ Disconnect =>
      proxyManager ! m
  } }

  def withDisconnected(nextReconnectTimeout: FiniteDuration = 1.seconds)(
      receive: Receive): Receive = receive.orElse[Any, Unit] {
    case Disconnected =>
      context.children.filterNot(proxyManager == _).foreach(context.stop(_))
      statusAlert = makeStatusAlert("danger").append(
        jQ("""<strong>You have been disconnected from the server.</strong>"""),
        jQ("""<span>Will try to reconnect in """+
            """<span class="reconnect-remaining-seconds"></span>"""+
            """ seconds. </span>""").append(
          jQ("""<a href="#">""").text("Reconnect now").click {
            (e: JQueryEventObject) =>
              self ! AttemptToConnect
          }
        )
      )
      jQ(".reconnect-remaining-seconds").text(nextReconnectTimeout.toSeconds.toString)
      val autoReconnectDeadline = nowDuration + nextReconnectTimeout
      context.setReceiveTimeout(1.seconds)
      context.become(waitingToAutoReconnect(
          autoReconnectDeadline, nextReconnectTimeout*2))
  }

  private def nowDuration = System.currentTimeMillis().milliseconds
}

class ProxyManager extends Actor {
  def receive = {
    case AttemptToConnect =>
      context.watch(context.actorOf(
          Props(new ClientProxy("ws://localhost:9000/chat-ws-entry", context.parent))))

    case Disconnect =>
      context.children.foreach(context.stop(_))

    case Terminated(proxy) =>
      context.parent ! Disconnected
  }
}

object MessagesContainer {
  val container = jQ(".msg-wrap")

  def clear(): Unit = container.empty()

  def addMessage(author: String, text: String, timestamp: js.Date,
      imageURL: String = null): Unit = {
    val timeStampStr = timestamp.toString()
    val optImageURL = Option(imageURL)
    val entry = jQ("""<div class="media msg">""")
    optImageURL foreach { url =>
      entry.append(
        jQ("""<a class="pull-left" href="#">""").append(
          jQ("""<img class="media-object" alt="gravatar" style="width: 32px; height: 32px">""").attr(
            "src", url
          )
        )
      )
    }
    entry.append(
      jQ("""<div class="media-body">""").append(
        jQ(s"""<small class="pull-right time"><i class="fa fa-clock-o"></i> $timeStampStr</small>"""),
        jQ("""<h5 class="media-heading">""").text(author),
        jQ("""<small class="col-lg-10">""").text(text)
      )
    )
    container.append(entry)
  }

  def addMessage(user: User, text: String, timestamp: js.Date): Unit =
    addMessage(user.nick, text, timestamp, null)

  def addMessage(msg: Message): Unit =
    addMessage(msg.user, msg.text, new js.Date(msg.timestamp))
}
