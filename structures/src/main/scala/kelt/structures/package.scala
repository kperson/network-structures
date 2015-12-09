package kelt


import akka.actor.ActorSystem

import kelt.structures.directory.DirectoryClient
import kelt.structures.lock.LockClient
import kelt.structures.pubsub.PubSubClient
import kelt.structures.queue.AsyncQueueClient


package object structures {

  class Clients(endpoint: String, actorSystem: Option[ActorSystem] = None) {

    val baseEndpoint = if(endpoint.endsWith("/")) endpoint else endpoint + "/"
    implicit lazy val system = actorSystem.getOrElse(ActorSystem("data-structures"))

    def directoryClient() = new DirectoryClient(baseEndpoint + "dir")

    def lockClient() = new LockClient(baseEndpoint + "lock")

    def queueClient(queue: String) = new AsyncQueueClient(queue, baseEndpoint + "queue")

    def pubSubClient(channel: String) = new PubSubClient(channel, baseEndpoint + "pubsub")

  }

  object Clients {

    def apply(endpoint: String) = new Clients(endpoint, None)


    def apply(endpoint: String, actorSystem: ActorSystem) = new Clients(endpoint, Some(actorSystem))

  }

}
