package udata.lock

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.Future
import scala.concurrent.duration._

import udata.http.TimeoutException
import udata.lock.LockManager._
import udata.util.TestUtils._


trait LockManagerSpec extends FlatSpec with Matchers with ScalaFutures {

  val defaultHoldTimeout = 10.seconds
  def displayName: String
  behavior of displayName

  def lockManager(system: ActorSystem): ActorRef

  it should "acquire a lock" in withActorSystem { system =>
    implicit val timeout = akka.util.Timeout(6.seconds)
    val resource = "r1"
    val manager = lockManager(system)
    val lockAcquisition = (manager ? LockAcquireRequest(resource, 3.seconds, defaultHoldTimeout))
    whenReady(lockAcquisition, 4.seconds) { x =>
      x should be (LockGrant(resource))
    }
  }

  it should "queue a lock" in withActorSystem { system =>
    implicit val timeout = akka.util.Timeout(6.seconds)
    import system.dispatcher
    val resource = "r1"
    val manager = lockManager(system)
    val l2 = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)).flatMap { case _ =>
      (manager ? LockAcquireRequest(resource, 1.seconds, defaultHoldTimeout))
    }

    whenReady(l2, 3.seconds) { x =>
      x shouldBe a [LockTimeout]
    }
  }

  it should "release a lock" in withActorSystem { system =>
    implicit val timeout = akka.util.Timeout(6.seconds)
    import system.dispatcher
    val resource = "r1"
    val manager = lockManager(system)

    val l1 = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)).asInstanceOf[Future[LockGrant]]
    l1.onSuccess { case x  => manager ! LockReleaseRequest(resource) }

    var release = false
    val l2 = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)).asInstanceOf[Future[LockGrant]]
    l2.onSuccess { case x  =>
      release = true
      manager ! LockReleaseRequest(resource) }

    val l3 = (manager ? LockAcquireRequest(resource, 3.seconds, defaultHoldTimeout)).map(x => true).recover {
      case TimeoutException(_) => false
    }

    whenReady(l3, 4.seconds) { x =>
      release should be (true)
      x should be (true)
    }
  }

  it should "handle dead letters" in withActorSystem { system =>
    implicit val timeout = akka.util.Timeout(6.seconds)
    import system.dispatcher
    val resource = "r1"
    val manager = lockManager(system)

    val l1 = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)).asInstanceOf[Future[LockGrant]]
    l1.onSuccess { case x  => manager ! LockReleaseRequest(resource) }

    manager ! LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)

    val l3 = (manager ? LockAcquireRequest(resource, 3.seconds, defaultHoldTimeout)).map(_ => true).recover {
      case TimeoutException(_) => false
    }

    whenReady(l3, 4.seconds) { x =>
      x should be (true)
    }
  }

  it should "timeout" in withActorSystem { system =>
    implicit val timeout = akka.util.Timeout(12.seconds)
    val resource = "rtimeout"
    val manager = lockManager(system)
    (manager ? LockAcquireRequest(resource, 3.seconds, defaultHoldTimeout))
    val lockAcquisitionShouldTimeout = (manager ? LockAcquireRequest(resource, 3.seconds, defaultHoldTimeout))


    whenReady(lockAcquisitionShouldTimeout, 5.seconds) { x =>
      x shouldBe a [LockTimeout]
    }
  }

}