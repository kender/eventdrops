package me.enkode.eventdrops.server

import scala.concurrent._, Future._, duration._
import akka.actor.{Props, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import spray.can.Http
import akka.util.Timeout
import akka.routing.SmallestMailboxPool

object Server extends App  {
  implicit val actorSystem = ActorSystem("eventdrops")

  val channels = actorSystem.actorOf(Props[Channels], "channels")

  val http = {
    val httpActor = actorSystem.actorOf(Props(classOf[HttpService], channels).withRouter(SmallestMailboxPool(8)), "http")

    implicit val askTimeout = Timeout(1.second)
    import actorSystem.dispatcher

    IO(Http).ask(Http.Bind(httpActor, "0.0.0.0", 8080)).flatMap {
      case bound: Http.Bound ⇒
        println("Listening on port 8080")
        successful(bound)
    }
  }
}