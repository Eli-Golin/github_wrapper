package boot

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import api.WrapperServiceAct
import spray.can.Http

/**
 * Created by eli on 08/11/15.
 */
object Boot extends App{

  implicit val system = ActorSystem("fdefault")

  // create and start isAlive notifier actor
  val service = system.actorOf(Props[WrapperServiceAct], "github-wrapper")

  // start HTTP server with rest service actor as a handler
  IO(Http) ! Http.Bind(service, "localhost", 8080)
}

