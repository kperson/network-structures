package udata.http

import java.io.OutputStream
import java.net.URL

import akka.actor.{Actor, ActorSystem}
import akka.io.IO

import spray.can.Http
import spray.http._

import scala.collection.mutable
import scala.concurrent.Promise

import udata.util._


class AsyncDownloader(url: URL, method: HttpMethod = HttpMethods.GET, headers: List[HttpHeader] = List.empty, outputStream: OutputStream, promise: Option[Promise[HttpResponse]] = None)(implicit actorSystem: ActorSystem) extends Actor {

  val buffer:scala.collection.mutable.Queue[WriteCommand] = mutable.Queue()
  IO(Http) ! Http.Connect(url.getHost, port = url.protocolAdjustedPort, sslEncryption = url.isSecure)

  def receive = {
    case Http.Connected(_, _) =>
      sender ! HttpRequest(method, url.getPath, headers = headers)
    case ChunkedResponseStart(response) =>
      promise.foreach { _.success(response) }
    case MessageChunk(data, _) =>
      outputStream.write(data.toByteArray)
    case ChunkedMessageEnd(_, _) =>
      outputStream.flush()
      outputStream.close()
      context.stop(self)
    case r @ HttpResponse(status, _, _, _) if status.intValue >= 400 =>
      promise.foreach { _.failure(FailedHttpResponse(r)) }
      context.stop(self)
    case r @ HttpResponse(status, _, _, _) if status.intValue < 400 =>
      val stream = r.asPartStream()
      stream.foreach { self ! _ }
  }

}