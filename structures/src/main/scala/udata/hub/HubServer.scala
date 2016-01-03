package udata.hub

import akka.actor.{Actor, Props}

import udata.HubServerConfig
import udata.count.CountServer
import udata.directory.{DirectoryServer, Directory}
import udata.lock.LockServer
import udata.pubsub.PubSubServer
import udata.queue.QueueServer


class HubServer(val directory: Directory, lockProps: Props, pubSubProps: Props, queueProps: Props, countProps: Props)
  extends PubSubServer
  with QueueServer
  with DirectoryServer
  with CountServer
  with LockServer
{

  lazy val lockManager = context.actorOf(lockProps)
  lazy val pubSubManager = context.actorOf(pubSubProps)
  lazy val queueManager = context.actorOf(queueProps)
  lazy val countManager = context.actorOf(countProps)

}