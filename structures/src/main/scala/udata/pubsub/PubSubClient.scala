package udata.pubsub

import java.net.URL

import akka.io.IO
import akka.actor._
import akka.io.Tcp.{CommandFailed, PeerClosed}

import scala.collection.mutable
import scala.concurrent.duration._

import spray.can.Http.Connect
import spray.can.Http
import spray.http._
import spray.http.HttpMethods._

import udata.util._

case object Send
case class BytePayload(bytes: Array[Byte])

case object PubSubConnectRetry
class PubSubPushActor(url: URL) extends Actor {

  import context.system
  import context.dispatcher

  val io = IO(Http)
  io ! Http.Connect(url.getHost, port = url.protocolAdjustedPort, sslEncryption = url.isSecure)
  var server: Option[ActorRef] = None
  val buffer:scala.collection.mutable.Queue[Array[Byte]] = mutable.Queue()

  def receive = {
    case Http.Connected(_, _)  =>
      server = Some(sender)
      sender ! ChunkedRequestStart(HttpRequest(POST, url.getPath))
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
      if(buffer.length == 1) {
        server.foreach { ref => self ! Send }
      }
    case PeerClosed =>
      //println("push peer closed")
      self ! PubSubConnectRetry
    case CommandFailed(Connect(_, _, _, _, _)) =>
      //println("push connection failed")
      self ! PubSubConnectRetry
    case PubSubConnectRetry =>
      //println("push retrying")
      context.system.scheduler.scheduleOnce(5.seconds) {
        io ! Http.Connect(url.getHost, port = url.protocolAdjustedPort, sslEncryption = url.isSecure)
      }
  }

}

class PubSubPullActor(url: URL, pubSub: PubSub[Array[Byte]]) extends Actor {


  import context.dispatcher
  import context.system

  val io = IO(Http)
  io ! Http.Connect(url.getHost, port = url.protocolAdjustedPort, sslEncryption = url.isSecure)

  def receive = {
    case Http.Connected(_, _)  =>
      val req = HttpRequest(GET, url.getPath)
      sender ! req
    case ex:MessageChunk =>
      pubSub.processIncoming(ex.data.toByteArray)
    case PeerClosed =>
      //println("pull peer closed")
      self ! PubSubConnectRetry
    case CommandFailed(Connect(_, _, _, _, _)) =>
      println("pull connection failed")
      self ! PubSubConnectRetry
    case PubSubConnectRetry =>
      //println("pull retrying")
      context.system.scheduler.scheduleOnce(5.seconds) {
        io ! Http.Connect(url.getHost, port = url.protocolAdjustedPort, sslEncryption = url.isSecure)
      }
  }

}


class PubSubClient(val channel: String, endpoint: String)(implicit system: ActorSystem) extends PubSub[Array[Byte]] {

  val baseURL = new URL(if(endpoint.endsWith("/")) endpoint else endpoint + "/")

  val url = new URL(baseURL, s"${channel}/")

  val push = system.actorOf(Props(new PubSubPushActor(url)))
  val pull = system.actorOf(Props(new PubSubPullActor(url, this)))

  override def publish(bytes: Array[Byte]) {
    push ! BytePayload(bytes)
  }

  override def close() {
    super.close()
    system.stop(push)
    system.stop(pull)
  }

}
