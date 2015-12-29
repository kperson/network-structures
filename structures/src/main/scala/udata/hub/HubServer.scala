package udata.hub

import akka.actor.{Actor, Props}

import udata.HubServerConfig
import udata.count.CountServer
import udata.directory.{DirectoryServer, Directory}
import udata.lock.LockServer
import udata.pubsub.{LocalPubSubManagerActor, PubSubServer}
import udata.queue.{AsyncQueueManagerActor, QueueServer}


class HubServer(val directory: Directory, val config: HubServerConfig)
  extends PubSubServer
  with QueueServer
  with DirectoryServer
  with CountServer
  with LockServer
{

  lazy val lockManager = context.actorOf(Props(Class.forName(config.lockManagerClassName).asInstanceOf[Class[Actor]]))
  lazy val pubSubManager = context.actorOf(Props[LocalPubSubManagerActor])
  lazy val queueManager = context.actorOf(Props[AsyncQueueManagerActor])
  lazy val countManager = context.actorOf(Props(Class.forName(config.countManagerClassName).asInstanceOf[Class[Actor]]))

}