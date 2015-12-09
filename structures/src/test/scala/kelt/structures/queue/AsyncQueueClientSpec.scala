package kelt.structures.queue

import akka.actor.ActorSystem

import kelt.structures.HubServerSpec
import kelt.structures.util.TestUtils._

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.duration._


class AsyncQueueClientSpec extends FlatSpec with Matchers with ScalaFutures with HubServerSpec {

  def withQueueClient(queue: String)(testCode: (AsyncQueueClient, ActorSystem) => Any): Unit = {
    withServer { (host, port) =>
      implicit val system = ActorSystem(randomActorId)
      val client = new AsyncQueueClient(queue, s"http://${host}:${port}/queue")
      testCode(client, system)
      system.shutdown()
    }
  }

  "QueueClient" should "enqueue and dequeue" in withQueueClient("TEST") { (client, system) =>
    import system.dispatcher
    val message = "TEST-MESSAGE-1"
    val dequeue = client.dequeue().map(new String(_))
    Thread.sleep(750.milliseconds.toMillis)
    client.enqueue(message.getBytes)
    whenReady(dequeue, 2.seconds) { qMsg =>
      qMsg should be (message)
    }
  }

}
