package udata.queue

import akka.actor.Actor

object AsyncQueueManagerActor {

  case class QueueSaveRequest(key: String, bytes: Array[Byte])
  case class QueueListenRequest(key: String)
  case class QueueListenResponse(key: String, listenerId: Long)
  case class DeQueueDataResponse(key: String, bytes: Array[Byte])
  case class RemoveQueueListener(key: String, listenerId: Long)

  lazy val queueManager = new AsyncQueueManager[Array[Byte]]()
}


trait AsyncQueueManagerActor extends Actor {

  import AsyncQueueManagerActor._

  def manager:AsyncQueueManager[Array[Byte]]

  def receive = {
    case QueueSaveRequest(key, bytes) => manager.save(key, bytes)
    case QueueListenRequest(key) =>
      val listener = sender
      val listenerId = manager.listen(key) { data =>
        listener ! DeQueueDataResponse(key, data)
      }
      listener ! QueueListenResponse(key, listenerId)
    case RemoveQueueListener(key, listenerId) =>
      manager.removeListener(key, listenerId)

  }

}