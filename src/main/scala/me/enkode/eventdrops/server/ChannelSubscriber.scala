package me.enkode.eventdrops.server

import akka.actor.{LoggingFSM, FSM, ActorLogging, Actor}
import spray.http._
import spray.http.HttpResponse
import spray.http.HttpHeaders._
import spray.http.CacheDirectives._
import spray.http.StatusCodes._

import spray.http.HttpHeaders.RawHeader
import spray.routing.RequestContext

object ChannelSubscriber {
  sealed trait State
  case object Initializing extends State
  case object Subscribed extends State

  sealed trait Data
  case object NoData extends Data
  case class Subscription(channel: String) extends Data

  sealed trait Message
  case class Subscribe(channel: String) extends Message

  case class SSEComment(msg: String) {
    override def toString = ": " + msg + "\r\n\r\n"
  }

  val initResponse: HttpResponse = {
    val body = HttpEntity(contentType = MediaType.custom("text/event-stream"), SSEComment("connection log stream opened").toString)
    val headers = List(
      `Cache-Control`(`no-cache`),
      `Connection`("Keep-Alive"),
      RawHeader("Access-Control-Allow-Origin","*")
    )
    HttpResponse(
      status = OK,
      headers = headers,
      entity = body
    )
  }

}

class ChannelSubscriber(requestContext: RequestContext)
  extends Actor with ActorLogging
  with FSM[ChannelSubscriber.State, ChannelSubscriber.Data] with LoggingFSM[ChannelSubscriber.State, ChannelSubscriber.Data] {
  import ChannelSubscriber._


  when(Initializing) {
    case Event(Subscribe(channel), _) ⇒
      requestContext.responder ! ChunkedResponseStart(initResponse)
      goto(Subscribed) using Subscription(channel)
  }

  when (Subscribed) {
    case Event(Channel.ChannelMessage(payload), _) ⇒
      requestContext.responder ! MessageChunk(payload.toString)
      stay()
  }

  startWith(Initializing, NoData)
  initialize()
}
