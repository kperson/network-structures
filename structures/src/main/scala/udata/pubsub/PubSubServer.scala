package udata.pubsub

import akka.actor.{Props, ActorRef, Actor}

import spray.can.Http
import spray.can.Http.RegisterChunkHandler
import spray.http._
import spray.io.CommandWrapper

import scala.concurrent.duration._

import udata.server.{SingleRequestBody, ChunkedRequestBody, BasicSprayServer}


case class AddListener(topicKey: String)
case class AddListenerResponse(key: String, listenerId: Long)
case class RemoveListener(key: String, listenerId: Long)

case class SaveRequest(key: String, bytes: Array[Byte])
case class ReceivedAck(key: String, dataId: Long, listenerId: Long)
case class PushedData(dataId: Long, payload: Array[Byte])

class PubSubManagerActor extends Actor {

  val manager = PubSubManagerActor.pubSubManager

  def receive = {
    case AddListener(key) =>
      val listener = sender
      val actorId = manager.addListener(key) { x =>
        listener ! PushedData(x.dataId, x.payload)
      }
      sender ! AddListenerResponse(key, actorId)
    case SaveRequest(key, bytes) =>
      manager.save(key, bytes)
    case ReceivedAck(key, dataId, listenerId) =>
      manager.waitForNext(key, dataId, listenerId)
    case RemoveListener(key, listenerId) =>
      manager.removeListener(key, listenerId)
  }

}

object PubSubManagerActor {
  lazy val pubSubManager = new PubSubManager[Array[Byte]]()
}


case class PubSubStreamerAck(dataId: Long)
case class PubSubStreamerConnect()

class PubSubSubscriberActor(client: ActorRef, manager: ActorRef, key: String, padding: Int = 0) extends Actor {

  client ! ChunkedResponseStart(HttpResponse(entity = " " * padding)).withAck(PubSubStreamerConnect())

  var actorId: Option[Long] = None

  def receive = {
    case PubSubStreamerConnect() =>
      println("push connection established")
      manager ! AddListener(key)
    case AddListenerResponse(_, listenerId) =>
      actorId = Some(listenerId)
    case c @ PushedData(dataId, bytes) =>
      client ! MessageChunk(bytes).withAck(PubSubStreamerAck(dataId))
    case PubSubStreamerAck(dataId) =>
      actorId.foreach { aId =>
          manager ! ReceivedAck(key, dataId, aId)
      }
    case ev: Http.ConnectionClosed =>
      println("closing subscribe channel")
      actorId.foreach { aId =>
        manager ! RemoveListener(key, aId)
      }

  }

}
class PubSubPublisherActor(client: ActorRef, manager: ActorRef, key: String, request: HttpRequest, chunked: Boolean) extends Actor {

  client ! CommandWrapper(SetRequestTimeout(Duration.Inf))

  if(!chunked) {
    val data = request.asPartStream().flatMap {
      case mc: MessageChunk => Some(mc.data.toByteArray)
      case _ => None
    }.flatten.toArray
    if(data.length != 0) {
      manager ! SaveRequest(key, data)
    }
    client ! HttpResponse(status = 204)
    context.stop(self)
  }

  def receive = {
    case mc: MessageChunk =>
      if(mc.data.length != 0) {
        manager ! SaveRequest(key, mc.data.toByteArray)
      }
    case ev: Http.ConnectionClosed =>
      context.stop(self)
  }
}


trait PubSubServer extends BasicSprayServer {

  def pubSubManager: ActorRef

  get("/pubsub/:key/?") { (params, client) =>
    val key = params("key").head
    context.actorOf(Props(new PubSubSubscriberActor(client, pubSubManager, key)))
  }

  get("/pubsub/:key/:padding/?") { (params, client) =>
    val key = params("key").head
    val padding = params("padding").head.toInt
    context.actorOf(Props(new PubSubSubscriberActor(client, pubSubManager, key, padding)))
  }

  post("/pubsub/:key/?") { (params, client, body) =>
    val key = params("key").head

    body match {
      case ChunkedRequestBody(ChunkedRequestStart(req)) =>
        val handler = context.actorOf(Props(new PubSubPublisherActor(client, pubSubManager, key, req, true)))
        sender ! RegisterChunkHandler(handler)
      case SingleRequestBody(req) =>
        context.actorOf(Props(new PubSubPublisherActor(client, pubSubManager, key, req, false)))
    }

  }

}
