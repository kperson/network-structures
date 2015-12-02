package kelt.structures.lock

import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import kelt.structures.http.TimeoutException

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.Future
import scala.concurrent.duration._


class LockManagerTests extends FlatSpec with Matchers with ScalaFutures   {

  val defaultHoldTimeout = 10.seconds

  "LockManager" should "acquire a lock" in {

    implicit val system = ActorSystem("l1")
    implicit val timeout = akka.util.Timeout(2.seconds)
    val resource = "r1"
    val manager = system.actorOf(Props(new LockManager))
    val lockAcquistion = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout))
    whenReady(lockAcquistion, Timeout(Span(2, Seconds))) { x =>
      x should be (LockGrant(resource))
    }

  }

  "LockManager" should "queue a lock" in {

    implicit val system = ActorSystem("l2")
    implicit val timeout = akka.util.Timeout(3.seconds)
    import system.dispatcher
    val resource = "r1"
    val manager = system.actorOf(Props(new LockManager))
    val l1 = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout))
    val l2 = (manager ? LockAcquireRequest(resource, 1.seconds, defaultHoldTimeout)).map {
      case _: LockGrant => true
      case _ => false
    }

    whenReady(l2, Timeout(Span(3, Seconds))) { x =>
      x should be (false)
    }
  }

  "LockManager" should "release a lock" in {
    implicit val system = ActorSystem("l3")
    implicit val timeout = akka.util.Timeout(10.seconds)
    import system.dispatcher
    val resource = "r1"
    val manager = system.actorOf(Props(new LockManager))

    val l1 = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)).asInstanceOf[Future[LockGrant]]
    l1.onSuccess { case x  => manager ! LockReleaseRequest(resource) }

    val l2 = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)).asInstanceOf[Future[LockGrant]]
    l2.onSuccess { case x  =>
      manager ! LockReleaseRequest(resource) }

    val l3 = (manager ? LockAcquireRequest(resource, 3.seconds, defaultHoldTimeout)).map(x => true).recover {
      case TimeoutException(_) => false
    }

    whenReady(l3, Timeout(Span(4, Seconds))) { x =>
      x should be (true)
    }
  }

  "LockManager" should "should handle dead letters" in {
    implicit val system = ActorSystem("l4")
    implicit val timeout = akka.util.Timeout(10.seconds)
    import system.dispatcher
    val resource = "r1"
    val manager = system.actorOf(Props(new LockManager))

    val l1 = (manager ? LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)).asInstanceOf[Future[LockGrant]]
    l1.onSuccess { case x  => manager ! LockReleaseRequest(resource) }

    manager ! LockAcquireRequest(resource, 2.seconds, defaultHoldTimeout)

    val l3 = (manager ? LockAcquireRequest(resource, 3.seconds, defaultHoldTimeout)).map(_ => true).recover {
      case TimeoutException(_) => false
    }

    whenReady(l3, Timeout(Span(4, Seconds))) { x =>
      x should be (true)
    }
  }

}
