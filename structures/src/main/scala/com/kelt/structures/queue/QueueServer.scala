package com.kelt.structures.queue

import akka.actor.{ActorRef, Actor, Props}
import com.kelt.structures.server.{SingleRequestBody, ChunkedRequestBody, BasicSprayServer}
import spray.can.Http
import spray.can.Http.RegisterChunkHandler
import spray.http._
import spray.io.CommandWrapper

import scala.concurrent.duration.Duration

case class QueueSaveRequest(key: String, bytes: Array[Byte])
case class QueueListenRequest(key: String)
case class QueueListenResponse(key: String, listenerId: Long)
case class DeQueueData(key: String, bytes: Array[Byte])
case class RemoveQueueListener(key: String, listenerId: Long)

class AsyncQueueManagerActor extends Actor {

  val manager = AsyncQueueManagerActor.queueManager

  def receive = {
    case QueueSaveRequest(key, bytes) => manager.save(key, bytes)
    case QueueListenRequest(key) =>
      val listener = sender
      val listenerId = manager.listen(key) { data =>
        listener ! DeQueueData(key, data)
      }
      sender ! QueueListenResponse(key, listenerId)
    case RemoveQueueListener(key, listenerId) =>
      manager.removeListener(key, listenerId)

  }

}


case class QueueListenConnect()

class QueueServerPushActor(client: ActorRef, manager: ActorRef, key: String, request: HttpRequest, chunked: Boolean) extends Actor {

  client ! CommandWrapper(SetRequestTimeout(Duration.Inf))

  if(!chunked) {
    val data = request.asPartStream().flatMap {
      case mc: MessageChunk => Some(mc.data.toByteArray)
      case _ => None
    }.flatten.toArray
    manager ! QueueSaveRequest(key, data)
    client ! HttpResponse(status = 204)
    context.stop(self)
  }

  def receive = {
    case mc: MessageChunk =>
      manager ! QueueSaveRequest(key, mc.data.toByteArray)
    case ev: Http.ConnectionClosed =>
      context.stop(self)
  }

}

class QueueServerPullActor(client: ActorRef, manager: ActorRef, key: String) extends Actor {

  client ! ChunkedResponseStart(HttpResponse()).withAck(QueueListenConnect())
  var listenerId: Option[Long] = None


  def receive = {
    case QueueListenConnect() =>
      manager ! QueueListenRequest(key)
    case QueueListenResponse(_, lId) => listenerId = Some(lId)
    case DeQueueData(_, data) =>
      client ! MessageChunk(data)
      client ! ChunkedMessageEnd()
      listenerId.foreach { lId =>
        manager ! RemoveQueueListener(key, lId)
      }
      context.stop(self)
    case ev: Http.ConnectionClosed =>
      listenerId.foreach { lId =>
        manager ! RemoveQueueListener(key, lId)
      }
    case x => println(s"queue pull: ${x}")
  }

}


object AsyncQueueManagerActor {

  lazy val queueManager = new AsyncQueueManager[Array[Byte]]()

}


trait QueueServer extends BasicSprayServer {

  def queueManager: ActorRef

  get(s"/queue/:key/?") { (params, client) =>
    val key = params("key").head
    context.actorOf(Props(new QueueServerPullActor(client, queueManager, key)))
  }

  post(s"/queue/:key/?") { (params, client, body) =>

    val key = params("key").head
    body match {
      case ChunkedRequestBody(ChunkedRequestStart(req)) =>
        val handler = context.actorOf(Props(new QueueServerPushActor(client, queueManager, key, req, true)))
        sender ! RegisterChunkHandler(handler)
      case SingleRequestBody(req) =>
        context.actorOf(Props(new QueueServerPushActor(client, queueManager, key, req, false)))
    }
  }

}
