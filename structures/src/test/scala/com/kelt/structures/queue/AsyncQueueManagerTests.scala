package com.kelt.structures.queue

import org.scalatest.{Matchers, FlatSpec}


class AsyncQueueManagerTests extends FlatSpec with Matchers {

  val topic = "hello"
  val message = "my message"

  var manager:AsyncQueueManager[String] = null

  "AsyncQueueManager" should "listen" in {
    manager = new AsyncQueueManager[String]()

    var ct = 0
    manager.listen(topic) { msg =>
      ct += 1
    }

    manager.listen(topic) { msg =>
      ct += 1
    }

    manager.save(topic, message)
    ct should be (1)
  }

  "AsyncQueueManager" should "buffer delivery" in {
    manager = new AsyncQueueManager[String]()
    var ct = 0
    manager.save(topic, message)

    manager.listen(topic) { msg =>
      ct += 1
    }

    manager.listen(topic) { msg =>
      ct += 1
    }

    ct should be (1)

  }

}
