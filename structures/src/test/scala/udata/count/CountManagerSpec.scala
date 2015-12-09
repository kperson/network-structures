package udata.count

import akka.actor.{Props, ActorSystem}
import akka.pattern.ask

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.Future
import scala.concurrent.duration._


class CountManagerSpec extends FlatSpec with Matchers with ScalaFutures {

  "CountManager" should "increment" in {
    implicit val system = ActorSystem("c1")
    val resource = "r1"
    implicit val timeout = akka.util.Timeout(3.seconds)
    import system.dispatcher
    val manager = system.actorOf(Props(new CountManager))
    val inc = (manager ? UpdateCountRequest(resource, 20, 1.minute)).flatMap { case x =>
      manager ? ResourceCountRequest(resource)
    }.asInstanceOf[Future[ResourceCountResponse]]
    whenReady(inc, Timeout(Span(3, Seconds))) { a =>
      a.count should be (20)
    }
  }

  "CountManager" should "timeout" in {
    implicit val system = ActorSystem("c2")
    val resource = "r1"
    implicit val timeout = akka.util.Timeout(3.seconds)
    import system.dispatcher
    val countTimeout = 200.milliseconds
    val manager = system.actorOf(Props(new CountManager))
    val inc = (manager ? UpdateCountRequest(resource, 20, countTimeout)).flatMap { case x =>
      Thread.sleep(countTimeout.toMillis + 100)
      manager ? ResourceCountRequest(resource)
    }.asInstanceOf[Future[ResourceCountResponse]]
    whenReady(inc, Timeout(Span(3, Seconds))) { a =>
      a.count should be (0)
    }
  }

  "CountManager" should "replace" in {
    implicit val system = ActorSystem("c2")
    val resource = "r1"
    implicit val timeout = akka.util.Timeout(3.seconds)
    import system.dispatcher
    val countTimeout = 10.seconds
    val manager = system.actorOf(Props(new CountManager))
    val inc = (manager ? UpdateCountRequest(resource, 20, countTimeout)).flatMap {
      case UpdateResponse(_, rKey, _) => manager ? UpdateCountRequest(resource, 30, countTimeout, Some(rKey))
      case x => Future.failed(new Exception(s"received ${x} excepted UpdateResponse"))
    }.flatMap { case _ =>
      manager ? ResourceCountRequest(resource)
    }.asInstanceOf[Future[ResourceCountResponse]]
    whenReady(inc, Timeout(Span(3, Seconds))) { a =>
      a.count should be (30)
    }
  }

  "CountManager" should "accumulate" in {
    implicit val system = ActorSystem("c2")
    val resource = "r1"
    implicit val timeout = akka.util.Timeout(3.seconds)
    import system.dispatcher
    val countTimeout = 10.seconds
    val manager = system.actorOf(Props(new CountManager))
    val inc = (manager ? UpdateCountRequest(resource, 20, countTimeout)).flatMap {
      case UpdateResponse(_, rKey, _) => manager ? UpdateCountRequest(resource, 30, countTimeout)
      case x => Future.failed(new Exception(s"received ${x} excepted UpdateResponse"))
    }.flatMap { case _ =>
      manager ? ResourceCountRequest(resource)
    }.asInstanceOf[Future[ResourceCountResponse]]
    whenReady(inc, Timeout(Span(3, Seconds))) { a =>
      a.count should be (50)
    }
  }

}
