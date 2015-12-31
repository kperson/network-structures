package udata.queue

import akka.actor.{ActorRef, Actor, Props}

import spray.can.Http
import spray.can.Http.RegisterChunkHandler
import spray.http._
import spray.io.CommandWrapper

import scala.concurrent.duration.Duration

import udata.server.{ChunkedRequestBody, SingleRequestBody, BasicSprayServer}


class QueueServerPushActor(client: ActorRef, manager: ActorRef, key: String, request: HttpRequest, chunked: Boolean) extends Actor {

  import AsyncQueueManagerActor._

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

object QueueServerPullActor {

  case object QueueListenConnect

}

class QueueServerPullActor(client: ActorRef, manager: ActorRef, key: String) extends Actor {

  import AsyncQueueManagerActor._
  import QueueServerPullActor._

  client ! ChunkedResponseStart(HttpResponse()).withAck(QueueListenConnect)
  var listenerId: Option[Long] = None


  def receive = {
    case QueueListenConnect =>
      manager ! QueueListenRequest(key)
    case QueueListenResponse(_, lId) => listenerId = Some(lId)
    case DeQueueDataResponse(_, data) =>
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


trait QueueServer extends BasicSprayServer {

  def queueManager: ActorRef

  get("/queue/:key/?") { (params, client) =>
    val key = params("key").head
    context.actorOf(Props(new QueueServerPullActor(client, queueManager, key)))
  }

  post("/queue/:key/?") { (params, client, body) =>

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
