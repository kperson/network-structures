package udata.hub

import akka.actor.Props
import udata.count.{CountManager, CountServer}
import udata.directory.{DirectoryServer, Directory}
import udata.lock.{LockManager, LockServer}
import udata.pubsub.{PubSubServer, PubSubManagerActor}
import udata.queue.{AsyncQueueManagerActor, QueueServer}


class HubServer(val directory: Directory)
  extends PubSubServer
  with QueueServer
  with DirectoryServer
  with CountServer
  with LockServer
{

  lazy val lockManager = context.actorOf(Props[LockManager])
  lazy val pubSubManager = context.actorOf(Props[PubSubManagerActor])
  lazy val queueManager = context.actorOf(Props[AsyncQueueManagerActor])
  lazy val countManager = context.actorOf(Props[CountManager])


}