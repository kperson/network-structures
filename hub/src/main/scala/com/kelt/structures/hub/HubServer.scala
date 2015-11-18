package com.kelt.structures.hub

import akka.actor.{Props, ActorSystem}

import com.kelt.structures.pubsub.{PubSubManagerActor, PubSubServer}
import com.kelt.structures.queue.{AsyncQueueManagerActor, QueueServer}
import com.kelt.structures.storage.{StorageServer, Storage}


class HubServer(sys: ActorSystem, val storage: Storage) extends StorageServer with PubSubServer with QueueServer {

  val pubSubManager = context.actorOf(Props[PubSubManagerActor])
  val queueManager = context.actorOf(Props[AsyncQueueManagerActor])

}