package udata.http

import java.net.URL

import akka.actor.{ActorSystem, ActorRef, Actor}
import akka.io.IO

import spray.can.Http
import spray.http._

import scala.collection.mutable
import scala.concurrent.Promise

import udata.util._

object AsyncUploader {

  case class RequestStarted(ref: ActorRef)

}

class AsyncUploader(url: URL, method: HttpMethod = HttpMethods.POST, headers: List[HttpHeader] = List.empty, promise: Option[Promise[HttpResponse]] = None)(implicit actorSystem: ActorSystem) extends Actor {

  import AsyncUploader._

  val buffer:scala.collection.mutable.Queue[WriteCommand] = mutable.Queue()
  var server: Option[ActorRef] = None

  IO(Http) ! Http.Connect(url.getHost, port = url.protocolAdjustedPort, sslEncryption = url.isSecure)


  def connected = server != None
  var writing = false

  def receive = {
    case Http.Connected(_, _) => {
      val client = sender
      sender ! ChunkedRequestStart(HttpRequest(method, url.getPath, headers = headers)).withAck(RequestStarted(client))
    }
    case r @ HttpResponse(status, _, _, _) if status.intValue < 400 =>
      promise.foreach { _.success(r) }
      context.stop(self)
    case r @ HttpResponse(_, _, _, _)  =>
      promise.foreach { _.failure(FailedHttpResponse(r)) }
      context.stop(self)
    case RequestStarted(ref) =>
      server = Some(ref)
      if(!buffer.isEmpty) {
        self ! SendTrigger
      }
    case SendTrigger =>
      server.foreach { ref =>
        if (!buffer.isEmpty) {
          writing = true
          buffer.dequeue() match {
            case SaveBytes(bytes) =>
              ref ! MessageChunk(bytes).withAck(SendTrigger)
            case CloseStorage =>
              ref ! ChunkedMessageEnd()
          }
        }
        else {
          writing = false
        }
      }
    case s: WriteCommand =>
      buffer.enqueue(s)
      if(connected && !writing && buffer.size == 1) {
        self ! SendTrigger
      }
  }

}