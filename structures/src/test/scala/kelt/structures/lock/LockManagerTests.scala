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

  implicit val system = ActorSystem("lock")

  "LockManager" should "acquire a lock" in {

    implicit val timeout = akka.util.Timeout(2.seconds)
    val resource = "r1"
    val manager = system.actorOf(Props(new LockManager))
    val lockAcquistion = (manager ? LockAcquireRequest(resource, 2.seconds))
    whenReady(lockAcquistion, Timeout(Span(2, Seconds))) { x =>
      x should be (Long.MinValue)
    }

  }

  "LockManager" should "queue a lock" in {

    implicit val timeout = akka.util.Timeout(3.seconds)
    import system.dispatcher
    val resource = "r1"
    val manager = system.actorOf(Props(new LockManager))
    val l1 = (manager ? LockAcquireRequest(resource, 2.seconds))
    val l2 = (manager ? LockAcquireRequest(resource, 1.seconds)).map(_ => true).recover {
      case TimeoutException(_) => false
    }

    whenReady(l2, Timeout(Span(3, Seconds))) { x =>
      x should be (false)
    }
  }

  "LockManager" should "release a lock" in {

    implicit val timeout = akka.util.Timeout(3.seconds)
    import system.dispatcher
    val resource = "r1"
    val manager = system.actorOf(Props(new LockManager))

    val l1 = (manager ? LockAcquireRequest(resource, 2.seconds)).asInstanceOf[Future[Long]]
    l1.onSuccess { case x  => manager ! LockReleaseRequest(resource, x) }

    val l2 = (manager ? LockAcquireRequest(resource, 2.seconds)).asInstanceOf[Future[Long]]
    l2.onSuccess { case x  => manager ! LockReleaseRequest(resource, x) }

    val l3 = (manager ? LockAcquireRequest(resource, 1.seconds)).map(x => Long.MinValue + 2 == x).recover {
      case TimeoutException(_) => false
    }

    whenReady(l3, Timeout(Span(3, Seconds))) { x =>
      x should be (true)
    }
  }

}
