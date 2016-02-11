package udata.server

import java.io.{OutputStream, InputStream}

import akka.actor.{ActorContext, Props, ActorRef, Actor}

import org.scalatra.{MultiParams, SinatraPathPatternParser}

import spray.can.Http
import spray.can.Http.RegisterChunkHandler
import spray.http.HttpHeaders.{RawHeader, `Content-Type`}
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.http._
import spray.io.CommandWrapper

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._

import udata.util._


class ByteReader(promise: Promise[Array[Byte]]) extends Actor {

  var buffer = ArrayBuffer[Byte]()

  def receive = {
    case c: MessageChunk =>
      c.data.toByteArray.foreach(buffer.append(_))
    case e: ChunkedMessageEnd =>
      promise.success(buffer.toArray)
  }

}


sealed trait RequestBody {

  def bytes(sender: ActorRef, context: ActorContext): Future[Array[Byte]] = {
    val promise = Promise[Array[Byte]]()
    this match {
      case ChunkedRequestBody(ChunkedRequestStart(req)) =>
        val handler = context.actorOf(Props(new ByteReader(promise)))
        sender ! RegisterChunkHandler(handler)
      case SingleRequestBody(req) =>
        val handler = context.actorOf(Props(new ByteReader(promise)))
        req.asPartStream().foreach(handler ! _)
    }
    promise.future
  }

}

case class ChunkedRequestBody(start: ChunkedRequestStart) extends RequestBody
case class SingleRequestBody(req: HttpRequest) extends RequestBody

object BasicSprayServer {

  type GETRequestHandler = (MultiParams, ActorRef) => Unit
  type POSTRequestHandler = (MultiParams, ActorRef, RequestBody) => Unit
  type PUTTRequestHandler = (MultiParams, ActorRef, MessageChunk) => Unit
  type DELETERequestHandler = (MultiParams, ActorRef, RequestBody) => Unit

  implicit class HttpRequestExtensions(self: HttpRequest) {

    def contentLength:Option[Long] = {
      self.headers.find(_.is("content-length")).map(_.value.toLong)
    }

  }
}


case class RequestMatch(method: HttpMethod, pattern: String) {


  lazy val sinatraPattern = SinatraPathPatternParser(pattern)

  def matches(req: HttpRequest, path: String) : Option[(MultiParams, List[HttpHeader])] = {
    sinatraPattern(path).flatMap { x => if (req.method == method) Some((x, req.headers)) else None  }
  }

}

class BasicSprayServer extends Actor {

  import BasicSprayServer._

  var gets = ListBuffer[(RequestMatch, GETRequestHandler)]()
  var posts = ListBuffer[(RequestMatch, POSTRequestHandler)]()
  var deletes = ListBuffer[(RequestMatch, DELETERequestHandler)]()

  def get(pattern: String)(handler: GETRequestHandler) = {
    gets.append((RequestMatch(GET, pattern), handler))
  }

  def post(pattern: String)(handler: POSTRequestHandler) = {
    posts.append((RequestMatch(POST, pattern), handler))
  }

  def delete(pattern: String)(handler: DELETERequestHandler) = {
    deletes.append((RequestMatch(DELETE, pattern), handler))
  }

  def send404(client: ActorRef) {
    client ! HttpResponse(404)
  }

  def receive = {
    case _: Http.Connected => sender ! Http.Register(self)
    case req @ HttpRequest(GET, Uri.Path(path), header, _, _) => {
      gets.flatMap { case (matcher, handler) =>
        matcher.matches(req, path).map { case (params, _) => (params, handler) }
      }.headOption match {
        case Some((params, handler)) =>
          handler(params, sender)
        case _ => send404(sender)
      }
    }

    case req @ HttpRequest(POST, Uri.Path(path), header, _, _) => {
      posts.flatMap { case (matcher, handler) =>
        matcher.matches(req, path).map { case (params, _) => (params, handler) }
      }.headOption match {
        case Some((params, handler)) =>
          handler(params, sender, SingleRequestBody(req))
        case _ => send404(sender)
      }
    }
    case req @ HttpRequest(DELETE, Uri.Path(path), header, _, _) => {
      deletes.flatMap { case (matcher, handler) =>
        matcher.matches(req, path).map { case (params, _) => (params, handler) }
      }.headOption match {
        case Some((params, handler)) =>
          handler(params, sender, SingleRequestBody(req))
        case _ => send404(sender)
      }
    }
    case chunk@ChunkedRequestStart(r) =>
      r match {
        case req@HttpRequest(POST, Uri.Path(path), header, _, _) => {
          posts.flatMap { case (matcher, handler) =>
            matcher.matches(req, path).map { case (params, _) => (params, handler) }
          }.headOption match {
            case Some((params, handler)) =>
              handler(params, sender, ChunkedRequestBody(chunk))
            case _ => send404(sender)
          }
        }
        case req@HttpRequest(DELETE, Uri.Path(path), header, _, _) => {
          deletes.flatMap { case (matcher, handler) =>
            matcher.matches(req, path).map { case (params, _) => (params, handler) }
          }.headOption match {
            case Some((params, handler)) =>
              handler(params, sender, ChunkedRequestBody(chunk))
            case _ => send404(sender)
          }
        }
      }
  }



}

case object StreamAck

case object MessageEndReceived
class Streamer(client: ActorRef, is: InputStream, contentType: Option[String] = None) extends Actor  {

  val mediaType = contentType.map(MediaType.custom(_)).getOrElse(`application/octet-stream`)
  val headers = List(`Content-Type`(ContentType(mediaType)), RawHeader("X-Type", "File"))

  client ! ChunkedResponseStart(HttpResponse(headers = headers, status = 200)).withAck(StreamAck)

  val s = is.stream(4096)
  val iter = s.iterator

  def receive = {
    case StreamAck =>
      if(iter.hasNext) {
        client ! MessageChunk(iter.next).withAck(StreamAck)
      }
      else {
        client ! ChunkedMessageEnd().withAck(MessageEndReceived)
      }
    case MessageEndReceived =>
      is.close()
      context.stop(self)
    case x: Http.ConnectionClosed =>
      self ! MessageEndReceived
  }
}


class ServerToSourceUploader(client: ActorRef, start: ChunkedRequestStart, out: OutputStream) extends Actor  {

  client ! CommandWrapper(SetRequestTimeout(Duration.Inf))

  def receive = {
    case c: MessageChunk =>
      out.write(c.data.toByteArray)
    case e: ChunkedMessageEnd =>
      out.close()
      client ! HttpResponse(status = 204)
      context.stop(self)
  }
}