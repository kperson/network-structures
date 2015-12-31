package udata.hub

import akka.actor.{Actor, Props}

import udata.HubServerConfig
import udata.count.CountServer
import udata.directory.{DirectoryServer, Directory}
import udata.lock.LockServer
import udata.pubsub.PubSubServer
import udata.queue.QueueServer


class HubServer(val directory: Directory, val config: HubServerConfig)
  extends PubSubServer
  with QueueServer
  with DirectoryServer
  with CountServer
  with LockServer
{

  lazy val lockManager = context.actorOf(Props(Class.forName(config.lockManagerClassName).asInstanceOf[Class[Actor]]))
  lazy val pubSubManager = context.actorOf(Props(Class.forName(config.pubSubManagerClassName).asInstanceOf[Class[Actor]]))
  lazy val queueManager = context.actorOf(Props(Class.forName(config.queueManagerClassName).asInstanceOf[Class[Actor]]))
  lazy val countManager = context.actorOf(Props(Class.forName(config.countManagerClassName).asInstanceOf[Class[Actor]]))

}