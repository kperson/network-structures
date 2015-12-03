package kelt.structures.http

import java.net.URL

import akka.actor.{ActorSystem, ActorRef, Actor}
import akka.io.IO

import kelt.structures.util._

import spray.can.Http
import spray.http._

import scala.collection.mutable
import scala.concurrent.Promise


class AsyncUploader(url: URL, method: HttpMethod = HttpMethods.POST, headers: List[HttpHeader] = List.empty, promise: Option[Promise[HttpResponse]] = None)(implicit actorSystem: ActorSystem) extends Actor {

  val buffer:scala.collection.mutable.Queue[WriteCommand] = mutable.Queue()
  IO(Http) ! Http.Connect(url.getHost, port = url.protocolAdjustedPort, sslEncryption = url.isSecure)
  var server: Option[ActorRef] = None

  def receive = {
    case Http.Connected(_, _) => {
      sender ! ChunkedRequestStart(HttpRequest(method, url.getPath, headers = headers))
      server = Some(sender)
      self ! SendTrigger
    }
    case r @ HttpResponse(status, _, _, _) if status.intValue < 400 =>
      promise.foreach { _.success(r) }
      context.stop(self)
    case r @ HttpResponse(_, _, _, _)  =>
      promise.foreach { _.failure(FailedHttpResponse(r)) }
      context.stop(self)
    case SendTrigger =>
      server.foreach { ref =>
        if (!buffer.isEmpty) {
          buffer.dequeue() match {
            case SaveBytes(bytes) => ref ! MessageChunk(bytes).withAck(SendTrigger)
            case CloseStorage => ref ! ChunkedMessageEnd()
          }
        }
      }
    case s: WriteCommand =>
      buffer.enqueue(s)
      self ! SendTrigger
  }

}