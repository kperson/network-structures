package com.kelt.structures.queue

import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import akka.io.IO
import akka.io.Tcp.{CommandFailed, PeerClosed}
import com.kelt.structures.storage.URLResolution

import spray.can.Http
import spray.can.Http.Connect
import spray.http.HttpMethods._
import spray.http.{MessageChunk, ChunkedRequestStart, HttpRequest}
import scala.concurrent.duration._


case object SendEnqueue
case class QueueSend(bytes: Array[Byte])


class AsyncQueueClientPushActor(res: URLResolution) extends Actor {

  import context.system

  val io = IO(Http)
  io ! Http.Connect(res.url.getHost, port = res.port, sslEncryption = res.isSecure)

  val buffer:scala.collection.mutable.Queue[Array[Byte]] = scala.collection.mutable.Queue[Array[Byte]]()
  var server: Option[ActorRef] = None

  def receive = {

    case Http.Connected(_, _)  =>
      server = Some(sender)
      sender ! ChunkedRequestStart(HttpRequest(POST, res.fullPath))
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
class AsyncQueueClientPullActor(res: URLResolution, handler: Array[Byte] => Unit) extends Actor {

  import context.system
  import context.dispatcher

  private var attempts = 0
  val io = IO(Http)
  self ! QueueConnectRetry(0.seconds)

  def receive = {
    case Http.Connected(_, _)  =>
      println(s"connected at ${System.currentTimeMillis}")
      val req = HttpRequest(GET, res.fullPath)
      if(attempts == 1) {
        sender ! req
      }
      else {
        context.system.scheduler.scheduleOnce(5.seconds) {
          sender ! req
        }
      }
    case ex:MessageChunk =>
      sender ! Http.Close
      handler(ex.data.toByteArray)
      context.stop(self)
    case PeerClosed =>
      self ! QueueConnectRetry(5.seconds)
    case CommandFailed(Connect(_, _, _, _, _)) =>
      self ! QueueConnectRetry(5.seconds)
    case QueueConnectRetry(delay) =>
      context.system.scheduler.scheduleOnce(delay) {
        attempts = attempts + 1
        println(s"attempting connection at ${System.currentTimeMillis}")
        io ! Http.Connect(res.url.getHost, port = res.port, sslEncryption = res.isSecure)
      }
    case x => println(x)
  }

}


class AsyncQueueClient(queue: String, queueURL: String)(implicit system: ActorSystem) extends AsyncQueue[Array[Byte]] {

  val res = URLResolution(queueURL, s"/${queue}/")
  val push = system.actorOf(Props(new AsyncQueueClientPushActor(res)))

  def enqueue(t: Array[Byte]) {
    push ! QueueSend(t)
  }

  def dequeue(handler: Array[Byte] => Unit) {
    system.actorOf(Props(new AsyncQueueClientPullActor(res, handler)))
  }

  def close() {
    system.stop(push)
  }

}
