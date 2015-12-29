package udata.pubsub


object PubSubManager {

  case class DataReceived[T](dataId: Long, payload: T)

  type DataId = Long
  type KeyName = String
  type ListenerId = Long
  type NumListener = Long

}

trait PubSubManager[T] {

  import PubSubManager._

  def removeListener(key: String, listenerId: Long)
  def save(key: String, payload: T)
  def waitForNext(key: String, dataId: Long, listenerId: Long)
  def addListener(key: String, autoAck:Boolean)(callback:(DataReceived[T]) => Unit) : Long
  def addListener(key: String)(callback:(DataReceived[T]) => Unit) : Long = addListener(key, false)(callback)
  def keys: List[String]

}