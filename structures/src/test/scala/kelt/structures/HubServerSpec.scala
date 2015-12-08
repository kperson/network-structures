package kelt.structures

import java.io.File
import java.net.ServerSocket
import java.util.UUID

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import kelt.structures.directory.file.FileDirectory
import kelt.structures.hub.HubServer
import org.scalatest.FlatSpec
import spray.can.Http

trait HubServerSpec extends FlatSpec {

  def randomActorId = UUID.randomUUID().toString.replace("-", "")

  def withServer(testCode: (String, Int) => Any) {


    val directory = File.createTempFile("dir", "")
    implicit val system = ActorSystem(randomActorId)
    import system.dispatcher

    val handler = system.actorOf(Props(
      new HubServer(
        new FileDirectory("root", directory)
      )).withDispatcher("akka.pubsub-dispatcher"))

    //get available port
    val socket = new ServerSocket(0)
    val openPort = socket.getLocalPort
    socket.close()

    IO(Http) ! Http.Bind(handler, interface = "0.0.0.0", port = openPort)
    try {
      testCode("0.0.0.0", openPort)
    }
    finally {
      system.shutdown()
    }

  }

}
