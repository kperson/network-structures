package udata.queue

import akka.actor.{Props, ActorRef, ActorSystem}

import org.scalatest.BeforeAndAfterAll

class TestLocalQueueManager(val manager: AsyncQueueManager[Array[Byte]]) extends AsyncQueueManagerActor

class LocalAsyncQueueManagerActorSpec extends AsyncQueueManagerActorSpec with BeforeAndAfterAll {

  def displayName = "Local Async Queue Manager Actor"

  val system: ActorSystem = ActorSystem("local-queue")

  def withManager(manager: AsyncQueueManager[Array[Byte]])(testCode: (ActorRef) => Unit) {
    val actorRef = system.actorOf(Props(new TestLocalQueueManager(manager)))
    testCode(actorRef)
    system.stop(actorRef)
  }

  override def afterAll() {
    super.afterAll()
    system.shutdown()
  }

}