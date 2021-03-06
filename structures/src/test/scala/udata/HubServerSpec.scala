package udata

import java.io.File
import java.net.ServerSocket
import java.util.UUID

import akka.actor.{Actor, Props, ActorSystem}
import akka.io.IO

import org.apache.commons.io.FileUtils
import org.scalatest.FlatSpec

import spray.can.Http

import udata.directory.system.FileSystemDirectory
import udata.hub.HubServer


trait HubServerSpec extends FlatSpec {

  def randomActorId = UUID.randomUUID().toString.replace("-", "")

  def withServer(testCode: (String, Int) => Any) {

    val directory = File.createTempFile("temp", System.nanoTime().toString)
    directory.delete()
    directory.mkdirs()

    implicit val system = ActorSystem(randomActorId)

    val serverConfig = new HubServerConfig()

    val lockProps = Props(Class.forName(serverConfig.lockManagerClassName).asInstanceOf[Class[Actor]])
    val pubSubProps = Props(Class.forName(serverConfig.pubSubManagerClassName).asInstanceOf[Class[Actor]])
    val queueProps = Props(Class.forName(serverConfig.queueManagerClassName).asInstanceOf[Class[Actor]])
    val countProps = Props(Class.forName(serverConfig.countManagerClassName).asInstanceOf[Class[Actor]])

    val handler = system.actorOf(Props(
      new HubServer(
        new FileSystemDirectory(directory),
        lockProps,
        pubSubProps,
        queueProps,
        countProps
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
      if(directory.exists() && directory.isDirectory) {
        FileUtils.deleteDirectory(directory)
      }
      system.shutdown()
    }

  }

}