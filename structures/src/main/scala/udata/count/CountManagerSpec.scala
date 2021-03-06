package udata.count

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.Future
import scala.concurrent.duration._

import udata.count.CountManager._
import udata.util.TestUtils._


trait CountManagerSpec extends FlatSpec with Matchers with ScalaFutures {

  def displayName: String
  behavior of displayName
  def countManager(system: ActorSystem): ActorRef

  it should "increment" in withActorSystem { system =>
    implicit val system = ActorSystem("c1")
    val resource = "r1"
    implicit val timeout = akka.util.Timeout(5.seconds)
    import system.dispatcher
    val manager = countManager(system)
    val inc = (manager ? UpdateCountRequest(resource, 20, 5.seconds)).flatMap { case x =>
      manager ? ResourceCountRequest(resource)
    }.asInstanceOf[Future[ResourceCountResponse]]
    whenReady(inc, 5.seconds) { a =>
      a.count should be (20)
    }
  }

  it should "timeout" in withActorSystem { system =>
    implicit val system = ActorSystem("c2")
    val resource = "r1"
    implicit val timeout = akka.util.Timeout(5.seconds)
    import system.dispatcher
    val countTimeout = 200.milliseconds
    val manager = countManager(system)
    val inc = (manager ? UpdateCountRequest(resource, 20, countTimeout)).flatMap { case x =>
      Thread.sleep(countTimeout.toMillis + 100)
      manager ? ResourceCountRequest(resource)
    }.asInstanceOf[Future[ResourceCountResponse]]

    whenReady(inc, 5.seconds) { a =>
      a.count should be (0)
    }
  }

  it should "replace" in withActorSystem { system =>
    val resource = "r1"
    implicit val timeout = akka.util.Timeout(5.seconds)
    import system.dispatcher
    val countTimeout = 10.seconds
    val manager = countManager(system)
    val inc = (manager ? UpdateCountRequest(resource, 20, countTimeout)).flatMap {
      case UpdateResponse(_, rKey, _) => manager ? UpdateCountRequest(resource, 30, countTimeout, Some(rKey))
      case x => Future.failed(new RuntimeException(s"received ${x} excepted UpdateResponse"))
    }.flatMap { case _ =>
      manager ? ResourceCountRequest(resource)
    }.asInstanceOf[Future[ResourceCountResponse]]

    whenReady(inc, 5.seconds) { a =>
      a.count should be (30)
    }
  }

  it should "accumulate" in withActorSystem { system =>
    implicit val system = ActorSystem("c2")
    val resource = "r1"
    implicit val timeout = akka.util.Timeout(5.seconds)
    import system.dispatcher
    val countTimeout = 10.seconds
    val manager = countManager(system)
    val inc = (manager ? UpdateCountRequest(resource, 20, countTimeout)).flatMap {
      case UpdateResponse(_, rKey, _) => manager ? UpdateCountRequest(resource, 30, countTimeout)
      case x => Future.failed(new Exception(s"received ${x} excepted UpdateResponse"))
    }.flatMap { case _ =>
      manager ? ResourceCountRequest(resource)
    }.asInstanceOf[Future[ResourceCountResponse]]
    whenReady(inc, 5.seconds) { a =>
      a.count should be (50)
    }
  }

}
