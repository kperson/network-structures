package kelt.structures.pubsub

case class Data[T](payload: T)
case class DataReceived[T](dataId: Long, payload: T)
case class WaitingFor[T](callback:(DataReceived[T]) => Unit, dataStatus: DataStatus)

sealed trait DataStatus
case class ReadyFor(dataId: Long) extends DataStatus
case class OnWire(dataId: Long) extends DataStatus


object PubSubManager {

  type DataId = Long
  type KeyName = String
  type ListenerId = Long
  type NumListener = Long

}

import PubSubManager._


class PubSubManager[T]  {

  type ListenerMap = scala.collection.mutable.HashMap[ListenerId, WaitingFor[T]]

  type ChannelMap = scala.collection.mutable.HashMap[DataId, Data[T]]
  type CountMap = scala.collection.mutable.HashMap[DataId, NumListener]

  type KeyMap = scala.collection.mutable.HashMap[KeyName, KeyMeta]

  case class KeyMeta(nextDataId: DataId, listeners: ListenerMap, data: ChannelMap, counts: CountMap)

  private var nextListenerId:Long = 0
  private val keyMap = new KeyMap()
  //private val lock: String = "1"

  private def next(key: String): Long = {
    val dataId = keyTable(key).nextDataId
    keyMap(key) = keyMap(key).copy(nextDataId = dataId + 1)
    dataId
  }

  private def lastDataId(key: String): Long = {
    keyTable(key).nextDataId
  }

  private def dataTable(key: String): ChannelMap = keyTable(key).data

  private def listenerCountTable(key: String) : CountMap = keyMap(key).counts


  private def updateListenerCountTable(key: String, dataId: DataId, count: Long)  {
    listenerCountTable(key)(dataId) = count
  }

  private def decListenerCountTable(key: String)(dataId: DataId)  {
    val nextDown = listenerCountTable(key).get(dataId).getOrElse(0L) - 1L
    if(nextDown <= 0) {
      listenerCountTable(key).remove(dataId)
      dataTable(key).remove(dataId)
    }
    else {
      listenerCountTable(key)(dataId) = nextDown
    }
  }


  private def listenerTable(key: String): ListenerMap = keyTable(key).listeners


  private def keyTable(key: String): KeyMeta = {
    keyMap.get(key) match {
      case Some(t) => t
      case _ =>
        keyMap(key) = KeyMeta(0, new ListenerMap(), new ChannelMap(), new CountMap())
        keyTable(key)
    }
  }

  private def startDataId(key: String) : Long = {
    val readyIds = listenerTable(key).flatMap { x =>
      x._2.dataStatus match {
        case ReadyFor(dId) => Some(dId)
        case _ => None
      }
    }
    if(readyIds.isEmpty) {
      keyTable(key).nextDataId
    }
    else {
      readyIds.min
    }
  }

  def addListener(key: String, autoAck:Boolean = false)(callback:(DataReceived[T]) => Unit) : Long = {
    //lock.synchronized {
      val listenerId = nextListenerId
      nextListenerId = nextListenerId + 1
      val start = startDataId(key)
      if (autoAck) {
        listenerTable(key)(listenerId) = WaitingFor({ data =>
          callback(data)
          waitForNext(key, data.dataId, listenerId)
        }, ReadyFor(start))
      }
      else {
        listenerTable(key)(listenerId) = WaitingFor(callback, ReadyFor(start))
        //println(s"add listener with ready for ${start}, size: ${keyTable(key).listeners.size}, key: ${key}")
      }

      val listenerCount = countForDataId(key, start)
      updateListenerCountTable(key, start, listenerCount)
      listenerId
    //}
  }

  private def countForDataId(key: String, dataId: Long): Int = {
    listenerTable(key).count { case (_, w) =>
      w match {
        case WaitingFor(_, ReadyFor(dId)) if dId <= dataId => true
        case WaitingFor(_, OnWire(dId)) if dId < dataId => true
        case _ => false
      }
    }
  }

  def save(key: String, payload: T) = {
    //lock.synchronized {
      val nextDataId = next(key)
      val listenerCount = countForDataId(key, nextDataId)
      updateListenerCountTable(key, nextDataId, listenerCount)
      dataTable(key)(nextDataId) = Data(payload)
      checkForDeliveryAndSend(nextDataId, key)
      nextDataId
    //}
  }

  def removeListener(key: String, listenerId: Long) {
    //lock.synchronized {
      val last = lastDataId(key)
      listenerTable(key)(listenerId).dataStatus match {
        case ReadyFor(dataId) =>
          for { x <- dataId to last } {
            decListenerCountTable(key)(x)
          }
          decListenerCountTable(key)(dataId)
        case OnWire(dataId) =>
          for { x <- (dataId + 1) to last } {
            decListenerCountTable(key)(x)
          }
      }
      listenerTable(key).remove(listenerId)

      //if nobody is listening, just delete all references to this channel
      if(listenerTable(key).isEmpty) {
        keyMap.remove(key)
      }
    //}
  }


  def waitForNext(key: String, dataId: Long, listenerId: Long) {
    val waitFor = listenerTable(key)(listenerId)
    listenerTable(key)(listenerId) = waitFor.copy(dataStatus = ReadyFor(dataId + 1))
    checkForDeliveryAndSend(dataId + 1, key)
  }


  private def checkForDeliveryAndSend(dataId: Long, key: String) {
    dataTable(key).get(dataId).foreach { d =>
      val matchStatus = ReadyFor(dataId)
      val listenerIds = listenerTable(key).filter { case (_, w) => w.dataStatus == matchStatus }.map { case (listenerId, w) => listenerId }

      if(!listenerIds.isEmpty) {
        val dataReceived = DataReceived(dataId, d.payload)
        listenerIds.foreach { lId =>
          val waitFor = listenerTable(key)(lId)
          decListenerCountTable(key)(dataId)
          listenerTable(key)(lId) = waitFor.copy(dataStatus = OnWire(dataId))
          waitFor.callback(dataReceived)
        }
      }
    }
  }

}
