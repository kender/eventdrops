package me.enkode.eventdrops.server

import akka.actor._

object Channels {
  sealed trait State
  case object Active extends State

  case class Data(channels: Map[String, ActorRef] = Map.empty)

  sealed trait Message
  case class CreateChannel(channelId: String) extends Message
  case class DestroyChannel(channelId: String) extends Message
  case class FindChannel(channelId: String) extends Message

  case class FoundChannel(channel: Option[ActorRef])
}
class Channels
  extends Actor with ActorLogging
  with FSM[Channels.State, Channels.Data] with LoggingFSM[Channels.State, Channels.Data] {

  import Channels._

  when(Active) {
    case Event(CreateChannel(channelId), data) ⇒
      val channelRef = context.system.actorOf(Props(classOf[Channel], channelId), channelId)
      stay() using data.copy(channels = data.channels + (channelId → channelRef))

    case Event(DestroyChannel(channelId), data) ⇒
      data.channels get channelId foreach { context.stop }
      stay() using data.copy(channels = data.channels - channelId)

    case Event(FindChannel(channelId), data) ⇒
      sender ! FoundChannel(data.channels get channelId)
      stay()
  }

  startWith(Active, Data())
  initialize()
}
