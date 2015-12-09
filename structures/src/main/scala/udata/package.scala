import akka.actor.ActorSystem

import udata.directory.DirectoryClient
import udata.lock.LockClient
import udata.pubsub.PubSubClient
import udata.queue.AsyncQueueClient

package object udata {

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
