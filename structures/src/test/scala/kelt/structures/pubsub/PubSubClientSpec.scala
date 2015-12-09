package kelt.structures.pubsub

import akka.actor.ActorSystem

import kelt.structures.HubServerSpec
import kelt.structures.util.TestUtils._

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Promise
import scala.concurrent.duration._


class PubSubClientSpec extends FlatSpec with Matchers with ScalaFutures with HubServerSpec {

  def withLockClient(channel: String)(testCode: (PubSubClient, ActorSystem) => Any): Unit = {
    withServer { (host, port) =>
      implicit val system = ActorSystem(randomActorId)
      val client = new PubSubClient(channel, s"http://${host}:${port}/pubsub")
      testCode(client, system)
      system.shutdown()
    }
  }

  "PubSubClient" should "publish and subscribe" in withLockClient("TEST") { (client, _) =>
    val message = "TEST-MESSAGE"
    val p = Promise[String]()
    client.foreach(a => p.success(new String(a)))
    Thread.sleep(750.milliseconds.toMillis)
    client.publish(message.getBytes)
    whenReady(p.future, 2.seconds) { pMsg =>
      pMsg should be (message)
    }
  }

}