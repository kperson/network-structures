package com.kelt.structures.pubsub

import akka.io.IO
import akka.actor._
import akka.io.Tcp.{CommandFailed, PeerClosed}
import com.kelt.structures.storage.URLResolution
import spray.can.Http.Connect

import scala.collection.mutable
import scala.concurrent.duration._

import spray.can.Http
import spray.http._
import spray.http.HttpMethods._


case object Send
case class BytePayload(bytes: Array[Byte])

case object PubSubConnectRetry
class PubSubPushActor(res: URLResolution) extends Actor {

  import context.system
  import context.dispatcher

  val io = IO(Http)
  io ! Http.Connect(res.url.getHost, port = res.port, sslEncryption = res.isSecure)
  var server: Option[ActorRef] = None
  val buffer:scala.collection.mutable.Queue[Array[Byte]] = mutable.Queue()

  def receive = {
    case Http.Connected(_, _)  =>
      server = Some(sender)
      HttpRequest(POST, res.fullPath)
      sender ! ChunkedRequestStart(HttpRequest(POST, res.fullPath))
      self ! Send
    case Send =>
      server.foreach { ref =>
        if (!buffer.isEmpty) {
          val data = buffer.dequeue()
          ref ! MessageChunk(data).withAck(Send)
        }
      }
    case p @ BytePayload(bytes) =>
      buffer.enqueue(bytes)
      if(buffer.length == 1 && server != None) {
        self ! Send
      }
    case PeerClosed =>
      println("push peer closed")
      self ! PubSubConnectRetry
    case CommandFailed(Connect(_, _, _, _, _)) =>
      println("push connection failed")
      self ! PubSubConnectRetry
    case PubSubConnectRetry =>
      println("push retrying")
      context.system.scheduler.scheduleOnce(5.seconds) {
        io ! Http.Connect(res.url.getHost, port = res.port, sslEncryption = res.isSecure)
      }
  }

}

class PubSubPullActor(res: URLResolution, pubSub: PubSub[Array[Byte]]) extends Actor {


  import context.dispatcher
  import context.system

  val io = IO(Http)
  io ! Http.Connect(res.url.getHost, port = res.port, sslEncryption = res.isSecure)

  def receive = {
    case Http.Connected(_, _)  =>
      val req = HttpRequest(GET, res.fullPath)
      sender ! req
    case ex:MessageChunk =>
      pubSub.processIncoming(ex.data.toByteArray)
    case PeerClosed =>
      println("pull peer closed")
      self ! PubSubConnectRetry
    case CommandFailed(Connect(_, _, _, _, _)) =>
      println("pull connection failed")
      self ! PubSubConnectRetry
    case PubSubConnectRetry =>
      println("pull retrying")
      context.system.scheduler.scheduleOnce(5.seconds) {
        io ! Http.Connect(res.url.getHost, port = res.port, sslEncryption = res.isSecure)
      }
  }

}


class PubSubClient(val channel: String, pubSubURL: String)(implicit system: ActorSystem) extends PubSub[Array[Byte]] {

  val res = URLResolution(pubSubURL, s"/${channel}/")
  val push = system.actorOf(Props(new PubSubPushActor(res)))
  val pull = system.actorOf(Props(new PubSubPullActor(res, this)))

  override def publish(bytes: Array[Byte]) {
    push ! BytePayload(bytes)
  }

  override def close() {
    super.close()
    system.stop(push)
    system.stop(pull)
  }

}
