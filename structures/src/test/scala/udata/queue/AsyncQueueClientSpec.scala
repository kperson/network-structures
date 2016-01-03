package udata.queue

import akka.actor.ActorSystem

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.duration._

import udata.HubServerSpec
import udata.util.TestUtils._

class AsyncQueueClientSpec extends FlatSpec with Matchers with ScalaFutures with HubServerSpec {

  behavior of "Queue Client"

  def withQueueClient(queue: String)(testCode: (AsyncQueueClient, ActorSystem) => Any): Unit = {
    withServer { (host, port) =>
      implicit val system = ActorSystem(randomActorId)
      val client = new AsyncQueueClient(queue, s"http://${host}:${port}/queue")
      testCode(client, system)
      system.shutdown()
    }
  }

  it should "enqueue and dequeue" in withQueueClient("TEST") { (client, system) =>
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
