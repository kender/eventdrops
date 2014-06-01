package me.enkode.eventdrops.server

import akka.actor._
import spray.http._
import spray.http.HttpResponse
import spray.http.HttpHeaders._
import spray.http.CacheDirectives._
import spray.http.StatusCodes._

import spray.http.HttpHeaders.RawHeader
import spray.routing.RequestContext
import spray.http.HttpResponse
import spray.routing.RequestContext
import spray.http.HttpHeaders.RawHeader
import spray.http.ChunkedResponseStart
import spray.can.Http
import akka.io.IO

object ChannelSubscriber {
  sealed trait State
  case object Initializing extends State
  case object Subscribed extends State
  case object Stopping extends State

  sealed trait Data
  case object NoData extends Data
  case class Subscription(channel: ActorRef) extends Data

  sealed trait Message
  case class Subscribe(channel: ActorRef) extends Message

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
    case Event(Subscribe(channelRef), _) ⇒
      channelRef ! Channel.Subscribe(Channel.Subscriber(self))
      requestContext.responder ! ChunkedResponseStart(initResponse)
      goto(Subscribed) using Subscription(channelRef)
  }

  when (Subscribed) {
    case Event(Channel.ChannelMessage(payload), _) ⇒
      requestContext.responder ! MessageChunk(payload.toString + "\n")
      stay()

    case Event(closed: Http.ConnectionClosed, Subscription(channelRef)) ⇒
      channelRef ! Channel.Unsubscribe(Channel.Subscriber(self))
      context stop self
      goto(Stopping)
  }

  when (Stopping) {
    case _ ⇒ stay()
  }

  startWith(Initializing, NoData)
  initialize()
}
