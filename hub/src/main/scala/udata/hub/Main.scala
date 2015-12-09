package udata.hub

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.io.IO

import spray.can.Http

import udata.directory.file.FileDirectory


case class ServerArguments(host: String = "0.0.0.0", port: Int = 8080, directoryPath: String = "/tmp/structure-path/directory")

object Main extends App  {


  def parser() = new scopt.OptionParser[ServerArguments]("hub") {

    head("web structures", "0.1")

    opt[String]("host") action { (x, c) =>
      c.copy(host = x)
    } text("the host the server will bind to")

    opt[Int]("port")  action { (x, c) =>
      c.copy(port = x)
    } text("the port the server will bind to")

    opt[String]("directory") action { (x, c) =>
      c.copy(directoryPath = x)
    }
  }

  parser().parse(args, ServerArguments()) match {
    case Some(config) =>

      implicit val system = ActorSystem("hub-server")
      import system.dispatcher

      val handler = system.actorOf(Props(
        new HubServer(
          new FileDirectory("root", new File(config.directoryPath))
        )).withDispatcher("akka.pubsub-dispatcher"))
      IO(Http) ! Http.Bind(handler, interface = config.host, port = config.port)
    case _ => println("--help for details")
  }

}