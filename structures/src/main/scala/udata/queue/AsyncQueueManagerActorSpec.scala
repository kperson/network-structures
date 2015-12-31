package udata.queue

import akka.actor.{Props, ActorSystem, Actor, ActorRef}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.Promise
import scala.concurrent.duration._

import udata.util.TestUtils._


trait AsyncQueueManagerActorSpec extends FlatSpec with Matchers with ScalaFutures {

  import AsyncQueueManagerActor._

  type ByteArray = Array[Byte]

  def withManager(manager: AsyncQueueManager[ByteArray])(testCode: (ActorRef) => Unit)
  def system: ActorSystem

  val testKey = "TEST-KEY"
  val testMessage = "TEST-MESSAGE"


  class EnqueueTestManager(promise: Promise[Int]) extends AsyncQueueManager[ByteArray] {

    override def save(key: String, t: ByteArray) {
      super.save(key, t)
      promise.success(1)
    }

  }

  val p1 = Promise[Int]()
  val m1 = new EnqueueTestManager(p1)
  "QueueManager" should "enqueue a message" in withManager(m1) { ref =>
    ref ! QueueSaveRequest(testKey, testMessage.getBytes)
    whenReady(p1.future, 5.seconds) { ct =>
      ct should be (1)
    }
  }


  class RemoveListenerTestManager(promise: Promise[Int]) extends AsyncQueueManager[ByteArray] {

    override def removeListener(key: String, listenerId: Long) {
      super.removeListener(key, listenerId)
      promise.success(1)
    }

  }

  val p2 = Promise[Int]()
  val m2 = new RemoveListenerTestManager(p2)
  "QueueManager" should "remove a listener" in withManager(m2) { ref =>
    ref ! RemoveQueueListener(testKey, 0)
    whenReady(p2.future, 3.seconds) { ct =>
      ct should be (1)
    }
  }


  class DequeueTestActor(promise: Promise[Int], manager: ActorRef) extends Actor {

    manager ! QueueListenRequest(testKey)

    def receive = {
      case DeQueueDataResponse(key, bytes) if key == testKey =>
        assert(new String(bytes) == testMessage)
        promise.success(1)
        context.stop(self)
      case QueueListenResponse(key, _) if key == testKey =>
        manager ! QueueSaveRequest(testKey, testMessage.getBytes)
    }

  }

  "QueueManager" should "deliver messages" in withManager(new AsyncQueueManager[ByteArray]) { ref =>
    val p = Promise[Int]()
    system.actorOf(Props(new DequeueTestActor(p, ref)))
    whenReady(p.future, 5.seconds) { ct =>
      ct should be (1)
    }
  }

}
