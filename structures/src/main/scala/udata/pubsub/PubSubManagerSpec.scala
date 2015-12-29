package udata.pubsub

import org.scalatest.{ Matchers, FlatSpec}


trait PubSubManagerSpec extends FlatSpec with Matchers {

  def pubSubManager(testCode:(PubSubManager[Array[Byte]]) => Any)

  val testKey = "myKey"
  val testKey2 = "myKey2"
  val payload = "myData"

  "PubSubManager" should "add a listener and broadcast" in pubSubManager { manager =>
    var receiveCt = 0
    manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
      data.payload should be (payload.getBytes)
    }
    manager.save(testKey, payload.getBytes)
    Thread.sleep(1500)
    receiveCt should be (1)
  }

  "PubSubManager" should "remove a listener" in pubSubManager { manager =>
    var receiveCt = 0
    val listenerId = manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
    }
    manager.removeListener(testKey, listenerId)
    Thread.sleep(1500)
    manager.save(testKey, payload.getBytes)
    receiveCt should be (0)
  }

  "PubSubManager" should "deliver data when received" in pubSubManager { manager =>
    var receiveCt = 0
    var listenerId: Long = 0
    listenerId = manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
      manager.waitForNext(testKey, data.dataId, listenerId)
    }
    manager.save(testKey, "bob".getBytes)
    manager.save(testKey, "bob".getBytes)
    Thread.sleep(1500)
    receiveCt should be (2)
  }

  "PubSubManager" should "deliver data when received and auto ack when set" in pubSubManager { manager =>
    var receiveCt = 0
    var listenerId: Long = 0
    listenerId = manager.addListener(testKey, true) { data =>
      receiveCt = receiveCt + 1
    }
    manager.save(testKey, "bob".getBytes)
    manager.save(testKey, "bob".getBytes)
    Thread.sleep(1500)
    receiveCt should be (2)
  }

  "PubSubManager" should "only deliver data on the listening channel" in pubSubManager { manager =>
    var receiveCt = 0
    manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
    }
    manager.save(testKey2, payload.getBytes)
    Thread.sleep(1500)
    receiveCt should be (0)
  }

  "PubSubManager" should "allow a subscribe to join mid stream" in pubSubManager { manager =>
    manager.save(testKey, payload.getBytes)

    var receiveCt = 0
    manager.addListener(testKey) { data =>
      receiveCt = receiveCt + 1
    }
    manager.save(testKey, payload.getBytes)
    Thread.sleep(1500)
    receiveCt should be (1)
  }

}
