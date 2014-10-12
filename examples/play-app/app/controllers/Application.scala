package controllers

import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.concurrent.Akka

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import akka.event.LoggingReceive

import akkajs.wsserver._

import models._


object Application extends Controller {

  import Play.current
  implicit val timeout = akka.util.Timeout(5.seconds)
  implicit def ec = Akka.system.dispatcher

  val chatManager = Akka.system.actorOf(Props[ChatManager], name = "chat")

  def index = Action {
    Ok(views.html.index())
  }

  val personForm: Form[Person] = Form {
  	mapping(
      "name" -> text
  	)(Person.apply)(Person.unapply)
  }

  var persons = List[Person]()

  def addPerson = Action { implicit request =>
  	val person = personForm.bindFromRequest.get
    persons ::= person
  	Redirect(routes.Application.index)
  }

  def getPersons = Action {
    Ok(Json.toJson(persons))
  }

  def chatWSEntry = ActorWebSocket { request =>
    chatManager ? NewConnection()
  }
}

case class NewConnection()

class ChatManager extends Actor with ActorLogging {
  RegisterPicklers.registerPicklers()

  import Play.current
  implicit def ec = Akka.system.dispatcher

  var client: ActorRef = _
  var periodicUpdates: Cancellable = _
  var cnt = 0

  def startPeriodicUpdates(user: User): Unit = {
    periodicUpdates = Akka.system.scheduler.schedule(500.milliseconds, 500.milliseconds) {
      cnt += 1
      client ! Message(user, s">>> Update $cnt <<<")
    }
  }

  def receive = LoggingReceive {
    case m @ NewConnection() =>
      log.info("chat manager, new connection")
      sender ! ActorWebSocket.actorForWebSocketHandler(self)

    case Connect(user) =>
      log.info(s"chat manager, user connected: $user")
      client = sender
      startPeriodicUpdates(user)

    case ToggleUpdates(user) =>
      if (periodicUpdates != null) {
        log.info("chat manager, pause updates")
        periodicUpdates.cancel()
        periodicUpdates = null
      } else {
        log.info("chat manager, resume updates")
        startPeriodicUpdates(user)
      }
  }
}
