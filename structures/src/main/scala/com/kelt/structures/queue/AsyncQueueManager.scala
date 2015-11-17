package com.kelt.structures.queue

class AsyncQueueManager[T] {

  val listeners = scala.collection.mutable.HashMap[String, scala.collection.mutable.Queue[(Long, T => Unit)]]()
  val data = scala.collection.mutable.HashMap[String, scala.collection.mutable.Queue[T]]()
  val random = new scala.util.Random

  def save(key: String, t: T) {
    dataQueue(key).enqueue(t)
    processQueue(key)
  }

  def listen(key: String)(handler:T => Unit) = {
    val listenerId = random.nextLong()
    listenerQueue(key).enqueue((listenerId, handler))
    processQueue(key)
    listenerId
  }

  def removeListener(key: String, listenerId: Long) {
    val filtered = listenerQueue(key).filter { case (lId, _) => listenerId != lId }
    listeners(key) = filtered
  }

  private def processQueue(key: String) {
    if(!dataQueue(key).isEmpty && !listenerQueue(key).isEmpty) {
      val (_, handler) = listenerQueue(key).dequeue()
      val value = dataQueue(key).dequeue()
      if(dataQueue(key).isEmpty) {
        data.remove(key)
      }
      if(listenerQueue(key).isEmpty) {
       listeners.remove(key)
      }
      handler(value)
    }
  }

  private def dataQueue(key: String) : scala.collection.mutable.Queue[T] = {
    data.get(key) match {
      case Some(t) => t
      case _ =>
        val map = scala.collection.mutable.Queue[T]()
        data(key) = map
        map
    }
  }

  private def listenerQueue(key: String) : scala.collection.mutable.Queue[(Long, T => Unit)] = {
    listeners.get(key) match {
      case Some(t) => t
      case _ =>
        val map = scala.collection.mutable.Queue[(Long, T => Unit)]()
        listeners(key) = map
        map
    }
  }

}
