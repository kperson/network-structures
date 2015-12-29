package udata.hub

import akka.actor.{ActorSystem, Props}
import akka.io.IO

import spray.can.Http
import udata.directory.Directory

import udata.{HubActorSystem, HubServerConfig}


case class ServerArguments(host: String = "0.0.0.0", port: Int = 8080)

object Server {

  implicit lazy val system = ActorSystem("hub-server")

}

object Main extends App  {


  def parser() = new scopt.OptionParser[ServerArguments]("hub") {

    head("web structures", "0.1")

    opt[String]("host") action { (x, c) =>
      c.copy(host = x)
    } text("the host the server will bind to")

    opt[Int]("port")  action { (x, c) =>
      c.copy(port = x)
    } text("the port the server will bind to")
  }

  parser().parse(args, ServerArguments()) match {
    case Some(config) =>

      implicit val system = HubActorSystem.system

      val serverConfig = new HubServerConfig()
      val directory = Class.forName(serverConfig.directoryManagerClassName).newInstance().asInstanceOf[Directory]

      val handler = system.actorOf(Props(
        new HubServer(
          directory,
          serverConfig
        )).withDispatcher("akka.pubsub-dispatcher"))
      IO(Http) ! Http.Bind(handler, interface = config.host, port = config.port)
    case _ => println("--help for details")
  }

}