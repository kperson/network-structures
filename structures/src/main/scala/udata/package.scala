import akka.actor.ActorSystem

import udata.directory.DirectoryClient
import udata.lock.LockClient
import udata.pubsub.PubSubClient
import udata.queue.AsyncQueueClient

package object udata {

  object HubActorSystem {

    lazy val system = ActorSystem("hub-system")

  }

  class UDataClients (endpoint: String)(implicit val system: ActorSystem) {

    val baseEndpoint = if(endpoint.endsWith("/")) endpoint else endpoint + "/"

    def directoryClient() = new DirectoryClient(baseEndpoint + "dir")

    def lockClient() = new LockClient(baseEndpoint + "lock")

    def queueClient(queue: String) = new AsyncQueueClient(queue, baseEndpoint + "queue")

    def pubSubClient(channel: String) = new PubSubClient(channel, baseEndpoint + "pubsub")

  }

  object UDataClients {

    def apply(endpoint: String)(implicit system: ActorSystem) = new UDataClients(endpoint)(system)

    implicit lazy val udataSystem = ActorSystem("udata")

  }

}
