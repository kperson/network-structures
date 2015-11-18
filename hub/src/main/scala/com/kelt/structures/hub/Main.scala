package com.kelt.structures.hub

import akka.actor.{ActorSystem, Props}
import akka.io.IO

import com.kelt.structures.storage.mem.MemStorage

import spray.can.Http


case class HostAndPort(host: String = "0.0.0.0", port: Int = 8080)

object Main extends App  {

  def parser() = new scopt.OptionParser[HostAndPort]("hub") {

    head("web structures", "0.1")

    opt[String]("host") action { (x, c) =>
      c.copy(host = x)
    } text("the host the server will bind to")

    opt[Int]("port")  action { (x, c) =>
      c.copy(port = x)
    } text("the port the server will bind to")
  }

  parser().parse(args, HostAndPort()) match {
    case Some(config) =>
      implicit val system = ActorSystem("hub-server")
      val handler = system.actorOf(Props(new HubServer(system, new MemStorage())).withDispatcher("akka.pubsub-dispatcher"))
      IO(Http) ! Http.Bind(handler, interface = config.host, port = config.port)
    case _ => println("--help for details")
  }

}