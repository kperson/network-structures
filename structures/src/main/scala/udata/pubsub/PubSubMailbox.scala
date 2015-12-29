package udata.pubsub

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config

import udata.pubsub.PubSubManagerActor._


class PubSubMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox (
  PriorityGenerator {
    case _: RemoveListenerRequest => 0
    case _: AddListenerRequest => 1
    case _: ReceivedAckRequest => 2
    case _: SaveRequest => 4
    case _ => 3
  }, 100)
