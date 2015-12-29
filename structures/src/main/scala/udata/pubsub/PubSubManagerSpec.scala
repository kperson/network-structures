package udata.pubsub

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, FlatSpec}

import scala.concurrent.Promise
import scala.concurrent.duration._

import udata.util.TestUtils._


trait PubSubManagerSpec extends FlatSpec with Matchers with ScalaFutures {

  def pubSubManager(testCode:(PubSubManager[Array[Byte]]) => Any)

  val testKey = "myKey"
  val testKey2 = "myKey2"
  val payload = "myData"

  "PubSubManager" should "add a listener and broadcast" in pubSubManager { manager =>
    var receiveCt = 0
    val promise = Promise[Int]()
    manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
      data.payload should be (payload.getBytes)
      promise.success(1)
    }
    manager.save(testKey, payload.getBytes)
    whenReady(promise.future, 5.seconds) { ct =>
      ct should be(1)
    }
  }

  "PubSubManager" should "remove a listener" in pubSubManager { manager =>
    var receiveCt = 0
    val promise = Promise[Int]()
    var listenerId = 0L
    listenerId = manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
      assert(receiveCt < 2)
      if(receiveCt == 1) {
        promise.success(1)
        manager.removeListener(testKey, listenerId)
        manager.save(testKey, payload.getBytes)
      }
    }
    manager.save(testKey, payload.getBytes)
    whenReady(promise.future, 5.seconds) { ct =>
      ct should be(1)
    }
  }

  "PubSubManager" should "deliver data when received" in pubSubManager { manager =>
    var receiveCt = 0
    var listenerId: Long = 0
    val promise = Promise[Int]()
    listenerId = manager.addListener(testKey) { data =>
      manager.waitForNext(testKey, data.dataId, listenerId)
      receiveCt = receiveCt + 1
      if(receiveCt == 2) {
        promise.success(2)
      }
    }
    manager.save(testKey, "bob".getBytes)
    manager.save(testKey, "bob".getBytes)
    whenReady(promise.future, 5.seconds) { ct =>
      ct should be (2)
    }
  }

  "PubSubManager" should "deliver data when received and auto ack when set" in pubSubManager { manager =>
    var receiveCt = 0
    var listenerId: Long = 0
    val promise = Promise[Int]()
    listenerId = manager.addListener(testKey, true) { data =>
      receiveCt = receiveCt + 1
      if(receiveCt == 2) {
        promise.success(2)
      }
    }
    manager.save(testKey, "bob".getBytes)
    manager.save(testKey, "bob".getBytes)

    whenReady(promise.future, 5.seconds) { ct =>
      ct should be(2)
    }
  }

  "PubSubManager" should "only deliver data on the listening channel" in pubSubManager { manager =>
    var receiveCt = 0
    manager.addListener(testKey) { data =>
      assert(true == false)
      receiveCt = receiveCt + 1
    }
    manager.save(testKey2, payload.getBytes)
    receiveCt should be (0)
  }

}
