package api

import akka.actor.Actor
import akka.event.Logging
import akka.event.slf4j.SLF4JLogging
import spray.http.StatusCodes._
import spray.http.{HttpRequest, HttpResponse}
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing.directives.LogEntry
import spray.routing._
import spray.client.pipelining._

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure => FutureFailure, Success => FutureSuccess, Try}

/**
 * REST Service actor.
 */

case class HumanInteraction(status:String,body:String,created_on:String)
case class StatusMsg(status:String,last_updated:String)
case class StatusHistory(status:StatusMsg,hmInteraction:Seq[HumanInteraction])

object GithubJsonProtocol extends DefaultJsonProtocol {
  implicit val hmInteractionFormat = jsonFormat3(HumanInteraction)
  implicit val stMsgFormat = jsonFormat2(StatusMsg)
  implicit val stHistFormat = jsonFormat2(StatusHistory)
}


class WrapperServiceAct extends Actor with WrapperService {

  implicit def actorRefFactory = context

  def receive = runRoute(routeWithLogging)

  // logs just the request method and response status at info level
  def requestMethodAndResponseStatusAsDebug(req: HttpRequest): Any => Option[LogEntry] = {
    case res: HttpResponse => Some(LogEntry("RequestMethod:" +req.method + req.uri +"\nRequestBody:"+req.message +
      "\nResponseStatus:"+res.message.status, Logging.InfoLevel))
    case _ => None // other kind of responses
  }

  def routeWithLogging = logRequestResponse(requestMethodAndResponseStatusAsDebug _)(route)
}

/**
 * REST Service
 */
trait WrapperService extends HttpService with SLF4JLogging {

  implicit val ec: ExecutionContext = actorRefFactory.dispatcher

  import SprayJsonSupport._
  import GithubJsonProtocol._
  import spray.json._

  val hmInteractionEndPoint: HttpRequest => Future[Seq[HumanInteraction]] = sendReceive ~> unmarshal[Seq[HumanInteraction]]
  val statusEndPoint : HttpRequest => Future[StatusMsg] = sendReceive ~> unmarshal[StatusMsg]

  val statusHistory: Future[StatusHistory] = for {
    stMsg <-statusEndPoint(Get("https://status.github.com/api/status.json"))
    hInteraction <- hmInteractionEndPoint(Get("https://status.github.com/api/messages.json"))
  } yield StatusHistory(stMsg,hInteraction)

  val aroundSpraysRejections = RejectionHandler {
    case Nil => complete(NotFound, "The url path does not match http://<hostname>:8080/api/<status.json/messages.json>")
    case MethodRejection(supported) :: _ => complete(MethodNotAllowed,s"The only supported http methods are: $supported")
    case MalformedRequestContentRejection(errorMsg, cause) :: _ => complete(BadRequest,s"Error:\n ${errorMsg}\nCause:\n${cause}")
  }

  val route = handleRejections(aroundSpraysRejections) {
    pathPrefix("api") {
      get {
        pathPrefix("status.json") {
          onComplete(statusEndPoint(Get("https://status.github.com/api/status.json"))) {
            case FutureSuccess(m @ StatusMsg(_,_)) => complete(m.toJson.toString)
            case FutureFailure(ex) => complete (InternalServerError,
              "The remote server:status.github.com has failed to response")
          }
        } ~
        pathPrefix("history.json") {
          onComplete(hmInteractionEndPoint(Get("https://status.github.com/api/messages.json"))) {
            case FutureSuccess(res) => complete(res.toJson.toString)
            case FutureFailure(ex) => complete (InternalServerError,
              "The remote server:status.github.com has failed to response")
          }
        }
      }~ pathPrefix("statusAndHistory.json") {
        onComplete(statusHistory) {
          case FutureSuccess(h @ StatusHistory(_,_)) => complete(h.toJson.toString)
          case FutureFailure(ex) => complete (InternalServerError,
            "The remote server:status.github.com has failed to response")
        }
      }~
      pathPrefix("checkApi.html") {
        compressResponse() {
          getFromResource("html/checkApi.html")
        }
      }
    }
  }
}