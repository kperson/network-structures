import akka.actor.ActorSystem

import udata.directory.DirectoryClient
import udata.lock.LockClient
import udata.pubsub.PubSubClient
import udata.queue.AsyncQueueClient

package object udata {

  object HubActorSystem {

    lazy val system = ActorSystem("hub-server")

  }

  class UDataClients (endpoint: String, actorSystem: Option[ActorSystem] = None) {

    implicit lazy val system = actorSystem.getOrElse(UDataClients.system)

    val baseEndpoint = if(endpoint.endsWith("/")) endpoint else endpoint + "/"

    def directoryClient() = new DirectoryClient(baseEndpoint + "dir")

    def lockClient() = new LockClient(baseEndpoint + "lock")

    def queueClient(queue: String) = new AsyncQueueClient(queue, baseEndpoint + "queue")

    def pubSubClient(channel: String) = new PubSubClient(channel, baseEndpoint + "pubsub")

  }

  object UDataClients {

    def apply(endpoint: String) = new UDataClients(endpoint, None)


    def apply(endpoint: String, actorSystem: ActorSystem) = new UDataClients(endpoint, Some(actorSystem))

    implicit lazy val system = ActorSystem("data-structures")

  }

}
