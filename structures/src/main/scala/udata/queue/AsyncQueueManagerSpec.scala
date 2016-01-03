package udata.queue

import org.scalatest.{Matchers, FlatSpec}


class AsyncQueueManagerSpec extends FlatSpec with Matchers {

  behavior of "Async Queue Manager"

  val topic = "hello"
  val message = "my message"

  it should "listen" in {
    val manager = new AsyncQueueManager[String]()

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

  it should "buffer delivery" in {
    val manager = new AsyncQueueManager[String]()
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
