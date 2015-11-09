package api

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import org.specs2.mutable.Specification
import serializables.StatusMsg
import spray.http.{HttpCharsets, MediaTypes, ContentType, HttpEntity}
import spray.testkit.Specs2RouteTest
import serializables.Implicits._
import spray.httpx.SprayJsonSupport._
import spray.http.StatusCodes._
import org.specs2.matcher.JsonMatchers
import scala.concurrent.duration.{Duration, DurationInt}

/**
 * Created by eli on 09/11/15.
 */

class WrapperServiceTest extends Specification with Specs2RouteTest with WrapperService{
  def actorRefFactory = system

  val duration = Duration(5, TimeUnit.SECONDS)
  implicit val routeTestTimeout = RouteTestTimeout(duration)

  val statusRequest = Get("/api/status.json")
  val historyRequest = Get("/api/history.json")
  val statusAndHistory = Get("/api/statusAndHistory.json")

  "The wrapper service" should {
    "respond with valid StatusMsg json when api/status.json is invoked" in {

      statusRequest ~> route ~> check {
        handled === true
        val returned = responseAs[String]
        returned contains "status"
        returned contains "last_updated"
        status === OK
      }
    }
  }


  "The wrapper service" should {
    "respond with valid HumanInteraction json when api/history.json is invoked" in {

      historyRequest ~> route ~> check {
        handled === true
        val returned = responseAs[String]
        returned contains "status"
        returned contains "created_on"
        returned contains "body"
        status === OK
      }
    }
  }

  "The wrapper service" should {
    "respond with valid StatusHistory json when api/history.json is invoked" in {

      statusAndHistory ~> route ~> check {
        handled === true
        val returned = responseAs[String]
        returned contains "status"
        returned contains "hmInteraction"
        returned contains "body"
        status === OK
      }
    }
  }
}

