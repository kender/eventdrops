package me.enkode.eventdrops.server

import akka.actor._

object Channel {
  case class Subscriber(actorRef: ActorRef)

  sealed trait State
  case object Active extends State

  case class Data(subscribers: Vector[Subscriber] = Vector.empty)

  sealed trait Message
  case class Subscribe(subscriber: Subscriber) extends Message
  case class Publish(message: ChannelMessage) extends Message

  case class ChannelMessage(payload: Any)
}

class Channel(id: String)
  extends Actor with ActorLogging
  with FSM[Channel.State, Channel.Data] with LoggingFSM[Channel.State, Channel.Data] {
  import Channel._

  when(Active) {
    case Event(Subscribe(subscriber), data) ⇒
      stay() using data.copy(subscribers = data.subscribers :+ subscriber)

    case Event(Publish(message), data) ⇒
      data.subscribers foreach { _.actorRef ! message }
      stay()
  }

  startWith(Active, Data())
  initialize()
}
