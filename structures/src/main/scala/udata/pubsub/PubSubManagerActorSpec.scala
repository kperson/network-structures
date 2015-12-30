package udata.pubsub

import akka.actor.{Props, Actor, ActorSystem, ActorRef}
import akka.pattern.ask

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.Promise
import scala.concurrent.duration._

import udata.pubsub.PubSubManager._
import udata.pubsub.PubSubManagerActor._
import udata.util.TestUtils._

trait PubSubManagerActorSpec extends FlatSpec with Matchers with ScalaFutures {

  def withManager(manager: PubSubManager[Array[Byte]])(testCode: (ActorRef) => Unit)
  def system: ActorSystem

  val testKey = "TEST-KEY"
  val testData = "TEST-DATA"

  class PublishTestThresholdManager(promise: Promise[Int], threshold: Int) extends LocalPubSubManager[Array[Byte]] {

    private var ct = 0

    override def save(key: String, payload: Array[Byte]) {
      super.save(key, payload)
      ct = ct + 1
      if(ct == threshold) {
        promise.success(threshold)
      }
      assert(ct <= threshold)
    }

  }
  val p1 = Promise[Int]()
  val t1 = new PublishTestThresholdManager(p1, 2)
  "PubSubManagerActor" should "queue data" in withManager(t1) { ref =>
    implicit val timeout = akka.util.Timeout(3.seconds)
    val sys = system
    import sys.dispatcher

    val addRequest = ref ? AddListenerRequest(testKey)
    addRequest.onSuccess { case _ =>
      ref ! SaveRequest(testKey, testData.getBytes)
      ref ! SaveRequest(testKey, testData.getBytes)
    }
    whenReady(p1.future, 5.seconds) { ct =>
      ct should be (2)
    }
  }


  class RemoveListenerTestManager(promise: Promise[Int]) extends LocalPubSubManager[Array[Byte]] {

    override def removeListener(key: String, listenerId: Long) {
      promise.success(1)
    }

  }
  val p2 = Promise[Int]()
  val t2 = new RemoveListenerTestManager(p2)
  "PubSubManagerActor" should "remove a listener" in withManager(t2) { ref =>
    ref ! RemoveListenerRequest(testKey, 0)
    whenReady(p2.future, 3.seconds) { ct =>
      ct should be (1)
    }
  }


  class AddListenerTestManager(promise: Promise[Int]) extends LocalPubSubManager[Array[Byte]] {

    override def addListener(key: String, autoAck:Boolean)(callback:(DataReceived[Array[Byte]]) => Unit) : Long = {
      val listenerId = super.addListener(key, autoAck)(callback)
      promise.success(1)
      listenerId
    }

  }
  val p3 = Promise[Int]()
  val t3 = new AddListenerTestManager(p3)
  "PubSubManagerActor" should "add a listener" in withManager(t3) { ref =>
    ref ! AddListenerRequest(testKey)
    whenReady(p3.future, 3.seconds) { ct =>
      ct should be (1)
    }
  }


  class ReceivedAckTestManager(promise: Promise[Int]) extends LocalPubSubManager[Array[Byte]] {

    override def waitForNext(key: String, dataId: Long, listenerId: Long) {
      super.waitForNext(key, dataId, listenerId)
      promise.success(1)
    }

  }
  val p4 = Promise[Int]()
  val t4 = new ReceivedAckTestManager(p4)
  "PubSubManagerActor" should "receive acknowledgements" in withManager(t4) { ref =>
    ref ! ReceivedAckRequest(testKey, 0, 0)
    whenReady(p4.future, 3.seconds) { ct =>
      ct should be (1)
    }
  }


  class ReceivedMessageTestActor(promise: Promise[Int], actorRef: ActorRef, threshold: Int) extends Actor {

    var ct = 0
    var listenerId: Option[Long] = None
    actorRef ! AddListenerRequest(testKey)

    def receive = {
      case x:PushedData =>
        listenerId.foreach { lId =>
          actorRef ! ReceivedAckRequest(testKey, x.dataId, lId)
        }
        assert(new String(x.payload) == testData)
        ct = ct + 1
        if (threshold == ct) {
          promise.success(threshold)
        }
      case AddListenerResponse(_, lId) =>
        listenerId = Some(lId)
        (0 until threshold).foreach { _ =>
          actorRef ! SaveRequest(testKey, testData.getBytes)
          actorRef ! SaveRequest(testKey, testData.getBytes)
        }
    }
  }

  "PubSubManagerActor" should "send data" in withManager(new LocalPubSubManager[Array[Byte]]) { ref =>
    val p5 = Promise[Int]()
    val threshold = 3
    system.actorOf(Props(new ReceivedMessageTestActor(p5, ref, threshold)))
    whenReady(p5.future, 5.seconds) { ct =>
      ct should be (threshold)
    }
  }

}
