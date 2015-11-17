package com.kelt.structures.demo

import akka.actor.{ActorSystem, Props}
import akka.io.IO

import com.kelt.structures.pubsub.{PubSubManagerActor, PubSubServer}
import com.kelt.structures.queue.{AsyncQueueManagerActor, QueueServer}
import com.kelt.structures.storage.StorageServer
import com.kelt.structures.storage.mem.MemStorage

import spray.can.Http

object Main extends App  {

  implicit val system = ActorSystem("hub-server")

  val hubPort = 8080
  val interface = "0.0.0.0"

  class HubServer(sys: ActorSystem) extends StorageServer with PubSubServer with QueueServer {

    val pubSubManager = context.actorOf(Props[PubSubManagerActor])
    val queueManager = context.actorOf(Props[AsyncQueueManagerActor])
    val storage = new MemStorage()
    implicit def system:ActorSystem = sys
  }

  val handler = system.actorOf(Props(new HubServer(system)).withDispatcher("akka.pubsub-dispatcher"))
  IO(Http) ! Http.Bind(handler, interface = interface, port = hubPort)

}
