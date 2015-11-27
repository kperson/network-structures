package kelt.structures.hub

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.io.IO

import kelt.structures.directory.file.FileDirectory
import kelt.structures.storage.file.FileStorage

import spray.can.Http


case class ServerArguments(host: String = "0.0.0.0", port: Int = 8080, storagePath: String = "/tmp/structure-path/storage", directoryPath: String = "/tmp/structure-path/directory")

object Main extends App  {


  def parser() = new scopt.OptionParser[ServerArguments]("hub") {

    head("web structures", "0.1")

    opt[String]("host") action { (x, c) =>
      c.copy(host = x)
    } text("the host the server will bind to")

    opt[Int]("port")  action { (x, c) =>
      c.copy(port = x)
    } text("the port the server will bind to")

    opt[String]("storage") action { (x, c) =>
      c.copy(storagePath = x)
    }

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
          new FileStorage(new File(config.storagePath)),
          new FileDirectory("root", new File(config.directoryPath))
        )).withDispatcher("akka.pubsub-dispatcher"))
      IO(Http) ! Http.Bind(handler, interface = config.host, port = config.port)
    case _ => println("--help for details")
  }

}