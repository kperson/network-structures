package udata.queue

import java.net.URL

import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import akka.io.IO
import akka.io.Tcp.{CommandFailed, PeerClosed}


import spray.can.Http
import spray.can.Http.Connect
import spray.http.HttpMethods._
import spray.http.{HttpResponse, MessageChunk, ChunkedRequestStart, HttpRequest}
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._

import udata.util._


case object SendEnqueue
case class QueueSend(bytes: Array[Byte])


class AsyncQueueClientPushActor(url: URL) extends Actor {

  import context.system

  val io = IO(Http)
  io ! Http.Connect(url.getHost, port = url.protocolAdjustedPort, sslEncryption = url.isSecure)

  val buffer:scala.collection.mutable.Queue[Array[Byte]] = scala.collection.mutable.Queue[Array[Byte]]()
  var server: Option[ActorRef] = None

  def receive = {

    case Http.Connected(_, _)  =>
      server = Some(sender)
      sender ! ChunkedRequestStart(HttpRequest(POST, url.toSprayUri))
      self ! SendEnqueue
    case SendEnqueue =>
      server.foreach { ref =>
        if (!buffer.isEmpty) {
          val data = buffer.dequeue()
          ref ! MessageChunk(data).withAck(SendEnqueue)
        }
      }
    case QueueSend(bytes) =>
      buffer.enqueue(bytes)
      if(buffer.length == 1 && server != None) {
        self ! SendEnqueue
      }
  }

}

case class QueueConnectRetry(delay: FiniteDuration)
class AsyncQueueClientPullActor(url: URL, promise: Promise[Array[Byte]]) extends Actor {

  import context.system

  IO(Http) ! Http.Connect(url.getHost, port = url.protocolAdjustedPort, sslEncryption = url.isSecure)

  def receive = {
    case Http.Connected(_, _)  =>
      println(s"connected at ${System.currentTimeMillis}")
      val req = HttpRequest(GET, url.toSprayUri)
      println(req)
      sender ! req
    case ex:MessageChunk =>
      sender ! Http.Close
      promise.success(ex.data.toByteArray)
      context.stop(self)
    case HttpResponse(status, entity, headers, _) =>
      if(!promise.isCompleted && status.intValue >= 400) {
        promise.failure(new RuntimeException("TODO"))
      }
      context.stop(self)
    case x => println(s"other: ${x}")
  }

}


class AsyncQueueClient(queue: String, endpoint: String)(implicit system: ActorSystem) extends AsyncQueue[Array[Byte]] {

  val baseURL = new URL(if(endpoint.endsWith("/")) endpoint else endpoint + "/")
  val url = new URL(baseURL, s"${queue}/")

  val push = system.actorOf(Props(new AsyncQueueClientPushActor(url)))

  def enqueue(t: Array[Byte]) {
    push ! QueueSend(t)
  }

  def dequeue() : Future[Array[Byte]] = {
    val p = Promise[Array[Byte]]()
    system.actorOf(Props(new AsyncQueueClientPullActor(url, p)))
    p.future
  }

  def close() {
    system.stop(push)
  }

}
