package kelt.structures.pubsub

import org.scalatest.{ Matchers, FlatSpec}


class PubSubManagerTests extends FlatSpec with Matchers {

  var manager: PubSubManager[String] = null
  val testKey = "myKey"
  val testKey2 = "myKey2"
  val payload = "myData"

  "PubSubManager" should "add a listener and broadcast" in {
    manager = new PubSubManager[String]()
    var receiveCt = 0
    manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
      data.payload should be (payload)
    }
    manager.save(testKey, payload)
    receiveCt should be (1)
  }

  "PubSubManager" should "remove a listener" in {
    manager = new PubSubManager[String]()
    var receiveCt = 0
    val listenerId = manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
    }
    manager.removeListener(testKey, listenerId)
    manager.save(testKey, payload)
    receiveCt should be (0)
  }

  "PubSubManager" should "deliver data when received" in {
    manager = new PubSubManager[String]()
    var receiveCt = 0
    var listenerId: Long = 0
    listenerId = manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
      manager.waitForNext(testKey, data.dataId, listenerId)
    }
    manager.save(testKey, "bob")
    manager.save(testKey, "bob")
    receiveCt should be (2)
  }

  "PubSubManager" should "deliver data when received and auto ack when set" in {
    manager = new PubSubManager[String]()
    var receiveCt = 0
    var listenerId: Long = 0
    listenerId = manager.addListener(testKey, true) { data =>
      receiveCt = receiveCt + 1
    }
    manager.save(testKey, "bob")
    manager.save(testKey, "bob")
    receiveCt should be (2)
  }

  "PubSubManager" should "only deliver data on the listening channel" in {
    manager = new PubSubManager[String]()
    var receiveCt = 0
    val listenerId = manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
    }
    manager.save(testKey2, payload)
    receiveCt should be (0)
  }

  "PubSubManager" should "allow a subscribe to join mid stream" in {
    manager = new PubSubManager[String]()
    manager.save(testKey, payload)

    var receiveCt = 0
    manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
    }
    manager.save(testKey, payload)
    receiveCt should be (1)
  }

}
