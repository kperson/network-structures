package udata.count

import akka.actor.ActorSystem

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.language.implicitConversions

import udata.HubServerSpec
import udata.util.TestUtils._


class CountClientSpec extends FlatSpec with Matchers with ScalaFutures with HubServerSpec {

  behavior of "Count Client"

  it should "increment count" in withServer { (host, port) =>
    implicit val system = ActorSystem(randomActorId)
    val client = new CountClient(s"http://${host}:${port}/count")
    val count = 10
    val inc = client.increment("r1", count, 3.seconds)
    whenReady(inc, 3.seconds) { rs =>
      rs.count should be (count)
    }
  }

  it should "fetch a count" in withServer { (host, port) =>
    implicit val system = ActorSystem(randomActorId)
    import system.dispatcher

    val client = new CountClient(s"http://${host}:${port}/count")
    val count = 10
    val inc = client.increment("r1", count, 3.seconds).flatMap { case _ => client.count("r1") }
    whenReady(inc, 3.seconds) { rs =>
      rs should be (count)
    }
  }

  it should "replace a key" in withServer { (host, port) =>
    implicit val system = ActorSystem(randomActorId)
    import system.dispatcher

    val client = new CountClient(s"http://${host}:${port}/count")
    val count = 10
    val newCount = 35
    val inc = client.increment("r1", count, 3.seconds)
      .flatMap {  x => client.increment("r1", newCount, 3.seconds, Some(x.replaceKey)) }
      .flatMap {  _ => client.count("r1") }

    whenReady(inc, 3.seconds) { rs =>
      rs should be (newCount)
    }
  }

  it should "expire" in withServer { (host, port) =>
    implicit val system = ActorSystem(randomActorId)
    import system.dispatcher

    val client = new CountClient(s"http://${host}:${port}/count")
    val count = 10
    val timeout = 100.milliseconds
    val inc = client.increment("r1", count, timeout)
      .flatMap {  _ =>
        Thread.sleep(timeout.toMillis + 100)
        client.count("r1")
      }

    whenReady(inc, 3.seconds) { rs =>
      rs should be (0)
    }
  }

  it should "accumulate" in withServer { (host, port) =>
    implicit val system = ActorSystem(randomActorId)
    import system.dispatcher

    val client = new CountClient(s"http://${host}:${port}/count")
    val count = 10
    val secondCount = 35
    val inc = client.increment("r1", count, 3.seconds)
      .flatMap {  x => client.increment("r1", secondCount, 3.seconds) }
      .flatMap {  _ => client.count("r1") }

    whenReady(inc, 3.seconds) { rs =>
      rs should be (count + secondCount)
    }
  }

}