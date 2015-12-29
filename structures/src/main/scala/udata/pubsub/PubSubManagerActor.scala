package udata.pubsub

import akka.actor.Actor


object PubSubManagerActor {

  case class AddListenerRequest(topicKey: String)

  case class AddListenerResponse(key: String, listenerId: Long)

  case class RemoveListenerRequest(key: String, listenerId: Long)

  case class SaveRequest(key: String, bytes: Array[Byte])

  case class ReceivedAckRequest(key: String, dataId: Long, listenerId: Long)

  case class PushedData(dataId: Long, payload: Array[Byte])

}

trait PubSubManagerActor extends Actor {

  import PubSubManagerActor._

  def manager:PubSubManager[Array[Byte]]

  def receive = {
    case AddListenerRequest(key) =>
      val listener = sender
      val actorId = manager.addListener(key) { x =>
        listener ! PushedData(x.dataId, x.payload)
      }
      sender ! AddListenerResponse(key, actorId)
    case SaveRequest(key, bytes) => manager.save(key, bytes)
    case ReceivedAckRequest(key, dataId, listenerId) => manager.waitForNext(key, dataId, listenerId)
    case RemoveListenerRequest(key, listenerId) => manager.removeListener(key, listenerId)
  }

}