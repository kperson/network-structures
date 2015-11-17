package com.kelt.structures.pubsub

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config

/**
 * Created by keltonperson on 10/17/15.
 */
class PubSubMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox (
  PriorityGenerator {
    case _: RemoveListener => 0
    case _: AddListener => 1
    case _: ReceivedAck => 2
    case _: SaveRequest => 4
    case _ => 3
  }, 100)
