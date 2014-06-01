package me.enkode.eventdrops.server

import scala.concurrent._, Future._, duration._

import akka.actor.{Props, ActorRef, ActorLogging}
import akka.pattern.ask

import spray.routing.{RequestContext, HttpServiceActor}
import spray.http.{StatusCode, StatusCodes}
import akka.util.Timeout

class HttpService(channelsRef: ActorRef) extends HttpServiceActor with ActorLogging {
  import context.dispatcher

  val channels =
    path("channel" / Segment ) { channelId: String ⇒
      put {
        complete {
          println(s"created: $channelId")
          createChannel(channelId)
        }
      } ~
      post {
        entity(as[String]) { entity ⇒
          complete {
            publish(channelId, entity)
          }
        }
      } ~
      get { reqContext ⇒
        println(s"subscribed to: $channelId")
        subscribe(channelId)(reqContext)
      }
    }


  def createChannel(channelId: String): Future[StatusCode] = successful {
    channelsRef ! Channels.CreateChannel(channelId)
    StatusCodes.Accepted
  }
  
  def subscribe(channelId: String)(requestContext: RequestContext) = {
    implicit val timeout = new Timeout(1.second)

    channelsRef.ask(Channels.FindChannel(channelId)) map {
      case Channels.FoundChannel(Some(channelRef)) ⇒
        val subscriber = context.actorOf(Props(classOf[ChannelSubscriber], requestContext))
        subscriber ! ChannelSubscriber.Subscribe(channelRef)
        subscriber

      case Channels.FoundChannel(None) ⇒ complete { StatusCodes.NotFound }
    }
  }

  def publish(channelId: String, entity: String): Future[StatusCode] = {
    implicit val timeout = new Timeout(1.second)

    println("publishing...")
    channelsRef.ask(Channels.FindChannel(channelId)) flatMap {
      case Channels.FoundChannel(Some(channelRef)) ⇒ successful {
        println("found... publishing")
        channelRef ! Channel.Publish(Channel.ChannelMessage(entity))
        StatusCodes.Accepted
      }

      case Channels.FoundChannel(None) ⇒ successful {
        println("not found...")
        StatusCodes.NotFound
      }
    }
  }
  
  override def receive = runRoute(channels)
}
