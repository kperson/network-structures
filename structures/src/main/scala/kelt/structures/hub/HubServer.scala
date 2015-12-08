package kelt.structures.hub

import akka.actor.Props

import kelt.structures.count.{CountServer, CountManager}
import kelt.structures.directory.{Directory, DirectoryServer}
import kelt.structures.lock.{LockServer, LockManager}
import kelt.structures.pubsub.{PubSubManagerActor, PubSubServer}
import kelt.structures.queue.{AsyncQueueManagerActor, QueueServer}


class HubServer(val directory: Directory)
  extends PubSubServer
  with QueueServer
  with DirectoryServer
  with LockServer
  with CountServer
{

  lazy val lockManager = context.actorOf(Props[LockManager])
  lazy val pubSubManager = context.actorOf(Props[PubSubManagerActor])
  lazy val queueManager = context.actorOf(Props[AsyncQueueManagerActor])
  lazy val countManager = context.actorOf(Props[CountManager])


}