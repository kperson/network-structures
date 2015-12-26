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

  def lockManager(system: ActorSystem): ActorRef


  "LockManager" should "acquire a lock" in withActorSystem { system =>
    implicit val timeout = akka.util.Timeout(3.seconds)
    val resource = "r1"
    val manager = lockManager(system)
    val lockAcquisition = (manager ? LockAcquireRequest(resource, 3.seconds, defaultHoldTimeout))
    whenReady(lockAcquisition, 4.seconds) { x =>
      x should be (LockGrant(resource))
    }
  }

  "LockManager" should "queue a lock" in withActorSystem { system =>
    implicit val timeout = akka.util.Timeout(3.seconds)
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

  "LockManager" should "release a lock" in withActorSystem { system =>
    implicit val timeout = akka.util.Timeout(10.seconds)
    import system.dispatcher
    val resource = "r1"
    val manager = lockManager(system)

    val l1 = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)).asInstanceOf[Future[LockGrant]]
    l1.onSuccess { case x  => manager ! LockReleaseRequest(resource) }

    val l2 = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)).asInstanceOf[Future[LockGrant]]
    l2.onSuccess { case x  =>
      manager ! LockReleaseRequest(resource) }

    val l3 = (manager ? LockAcquireRequest(resource, 3.seconds, defaultHoldTimeout)).map(x => true).recover {
      case TimeoutException(_) => false
    }

    whenReady(l3, 4.seconds) { x =>
      x should be (true)
    }
  }

  "LockManager" should "should handle dead letters" in withActorSystem { system =>
    implicit val timeout = akka.util.Timeout(10.seconds)
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

}