package udata.directory

import akka.actor.ActorSystem

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}
import udata.http.ResourceNotFoundException

import scala.concurrent.duration._

import udata.HubServerSpec
import udata.util._
import udata.util.TestUtils._


class DirectoryClientSpec extends FlatSpec with Matchers with ScalaFutures with HubServerSpec {


  def withDirectoryClient(testCode: (DirectoryClient, ActorSystem) => Any): Unit = {
    withServer { (host, port) =>
      implicit val system = ActorSystem(randomActorId)
      val client = new DirectoryClient(s"http://${host}:${port}/dir")
      try {
        testCode(client, system)
      }
      finally {
        system.shutdown()
      }
    }
  }

  "DirectoryClient" should "add a file" in withDirectoryClient { (client, system) =>
    val contents = "FILE-CONTENTS"
    val filePath = "/parent/hello.txt"
    val stream = client.addFile(filePath)
    stream.write(contents.getBytes)
    stream.close()

    Thread.sleep(700.milliseconds.toMillis)
    val fetch = client.fetch(filePath)
    whenReady(fetch, 2.seconds) { con =>
      con shouldBe a [FileContent]
      val fetchedContents = con.asInstanceOf[FileContent]
      val fileContents = new String(fetchedContents.inputStream.stream(4096).flatten.toArray)
      fileContents should be (contents)
    }
  }


  "Directory Client" should "fetch directory contents" in withDirectoryClient { (client, system) =>
    val contents = "FILE-CONTENTS"
    val filePath = "/parent/hello.txt"
    val stream = client.addFile(filePath)
    stream.write(contents.getBytes)
    stream.close()

    val filePath2 = "/parent/p1/hello2.txt"
    val stream2 = client.addFile(filePath2)
    stream2.write(contents.getBytes)
    stream2.close()

    Thread.sleep(700.milliseconds.toMillis)
    val fetch = client.fetch("/parent")
    whenReady(fetch, 2.seconds) { con =>
      con should be (DirectoryContent((DirectoryListing(List("hello.txt"), List("p1")))))
    }
  }


  "Directory Client" should "should fail if no file is found" in withDirectoryClient { (client, system) =>
    val filePath = "/parent/hello.txt"
    val fetch = client.fetch(filePath)
    whenReady(fetch.failed, 3.seconds) { con =>
      con shouldBe a [ResourceNotFoundException]
    }
  }

  "Directory Client" should "should fail if no directory is found" in withDirectoryClient { (client, system) =>
    val filePath = "/parent"
    val fetch = client.fetch(filePath)
    whenReady(fetch.failed, 3.seconds) { con =>
      con shouldBe a [ResourceNotFoundException]
    }
  }

  "Directory Client" should "delete a file" in withDirectoryClient { (client, system) =>
    import system.dispatcher
    val contents = "FILE-CONTENTS"
    val filePath = "/parent/hello.txt"
    val stream = client.addFile(filePath)
    stream.write(contents.getBytes)
    stream.close()

    Thread.sleep(600.milliseconds.toMillis)
    val fetch = client.delete(filePath).flatMap { _ =>
      Thread.sleep(300.milliseconds.toMillis)
      client.fetch(filePath)
    }
    whenReady(fetch.failed, 2.seconds) { con =>
      con shouldBe a [ResourceNotFoundException]
    }
  }

}