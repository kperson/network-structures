package udata.pubsub

import akka.actor.{Props, ActorSystem, ActorRef}
import org.scalatest.BeforeAndAfterAll


class TestLocalManager(val manager: PubSubManager[Array[Byte]]) extends PubSubManagerActor

class LocalPubSubManagerActorSpec extends PubSubManagerActorSpec with BeforeAndAfterAll {

  val system: ActorSystem = ActorSystem("local-pub-sub")

  def withManager(manager: PubSubManager[Array[Byte]])(testCode: (ActorRef) => Unit) {
    val actorRef = system.actorOf(Props(new TestLocalManager(manager)))
    testCode(actorRef)
    system.stop(actorRef)
  }

  override def afterAll() {
    super.afterAll()
    system.shutdown()
  }

}