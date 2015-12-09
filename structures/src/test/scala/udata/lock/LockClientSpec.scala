package udata.lock

import akka.actor.ActorSystem
import udata.HubServerSpec
import udata.http.TimeoutException

import udata.structures.HubServerSpec
import udata.structures.http.TimeoutException
import udata.structures.util.TestUtils._

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.duration._


class LockClientSpec extends FlatSpec with Matchers with ScalaFutures with HubServerSpec {

  def withLockClient(testCode: (LockClient, ActorSystem) => Any): Unit = {
    withServer { (host, port) =>
      implicit val system = ActorSystem(randomActorId)
      val client = new LockClient(s"http://${host}:${port}/lock")
      testCode(client, system)
      system.shutdown()
    }
  }

  "LockClient" should "lock a resource" in withLockClient { (client, system) =>
    val targetResource = "TEST-RESOURCE-1"
    val lockAcquire = client.lock(targetResource, 2.seconds, 2.seconds)
    whenReady(lockAcquire, 2.second) { resource =>
      resource should be (targetResource)
    }
  }

  "LockClient" should "auto unlock after expiration" in withLockClient { (client, system) =>
    import system.dispatcher
    val targetResource = "TEST-RESOURCE-2"
    val lockHoldDuration = 1.second
    val lockAcquire = client.lock(targetResource, 2.seconds, lockHoldDuration).flatMap { _ =>
      Thread.sleep(lockHoldDuration.toMillis + 100)
      client.lock(targetResource, 1.millisecond, 2.seconds)
    }
    whenReady(lockAcquire, 2.second) { resource =>
      resource should be (targetResource)
    }
  }

  "LockClient" should "should timeout" in withLockClient { (client, system) =>
    import system.dispatcher
    val targetResource = "TEST-RESOURCE-3"
    val lockHoldDuration = 1.second
    val acquireTimeout = 1.second
    val lockAcquire = client.lock(targetResource, acquireTimeout, lockHoldDuration).flatMap { _ =>
      //only wait for have the time the lock will hold, should cause a timeout
      client.lock(targetResource, lockHoldDuration / 2, 2.seconds)
    }
    whenReady(lockAcquire.failed, 2.second) { resource =>
      resource shouldBe a [TimeoutException]
    }
  }

  "LockClient" should "should manually unlock" in withLockClient { (client, system) =>
    import system.dispatcher
    val targetResource = "TEST-RESOURCE"
    val lockHoldDuration = 10.second
    val acquireTimeout = 1.second
    val lockAcquire = client.lock(targetResource, acquireTimeout, lockHoldDuration).flatMap { _ =>
      client.unlock(targetResource)
    }.flatMap { _ =>
      client.lock(targetResource, 1.millisecond, 2.seconds)
    }
    whenReady(lockAcquire, 2.second) { resource =>
      resource should be (targetResource)
    }
  }

}
