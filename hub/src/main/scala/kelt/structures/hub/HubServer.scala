package kelt.structures.hub

import akka.actor.{Props, ActorSystem}
import kelt.structures.directory.{Directory, DirectoryServer}

import kelt.structures.pubsub.{PubSubManagerActor, PubSubServer}
import kelt.structures.queue.{AsyncQueueManagerActor, QueueServer}
import kelt.structures.storage.{StorageServer, Storage}


class HubServer(val storage: Storage, val directory: Directory)
  extends StorageServer
  with PubSubServer
  with QueueServer
  with DirectoryServer
{

  lazy val pubSubManager = context.actorOf(Props[PubSubManagerActor])
  lazy val queueManager = context.actorOf(Props[AsyncQueueManagerActor])

}