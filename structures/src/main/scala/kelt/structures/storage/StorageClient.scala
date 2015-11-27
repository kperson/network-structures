package kelt.structures.storage

import java.io._
import java.net.URL

import akka.actor._
import akka.io.IO

import kelt.structures.http.ResourceNotFoundException
import kelt.structures.util._

import spray.can.Http
import spray.http._
import spray.http.HttpMethods._

import scala.concurrent.{Promise, Future}


case class ManagedConnection(host: String, port: Int, path: String, method: spray.http.HttpMethod, isSecure: Boolean)

case class UploadConfirm(ref: ActorRef)

class Uploader(inputStream: InputStream, mc: ManagedConnection, promise: Promise[Unit])(implicit system: ActorSystem) extends Actor {

  val io = IO(Http)
  io ! Http.Connect(mc.host, port = mc.port, sslEncryption = mc.isSecure)
  val s = inputStream.stream(4096).iterator


  def receive = {
    case Http.Connected(_, _)  =>
      val req = HttpRequest(mc.method, mc.path)
      sender ! ChunkedRequestStart(req).withAck(UploadConfirm(sender))
    case HttpResponse(status, _, _, _) if status.intValue < 400 =>
      inputStream.close()
      promise.success(Unit)
      context.stop(self)
    case UploadConfirm(ref) =>
      if(s.hasNext) {
        ref ! MessageChunk(s.next()).withAck(UploadConfirm(ref))
      }
      else {
        ref ! ChunkedMessageEnd()
      }

  }

}

class Downloader(key: String, outStream: OutputStream, mc: ManagedConnection, promise: Promise[Unit])(implicit system: ActorSystem) extends Actor {

  val io = IO(Http)
  io ! Http.Connect(mc.host, port = mc.port, sslEncryption = mc.isSecure)

  def receive = {
    case Http.Connected(_, _) =>
      sender ! HttpRequest(mc.method, mc.path)
    case HttpResponse(status, _, _, _) if status.intValue == 404 =>
      outStream.flush()
      outStream.close()
      promise.failure(ResourceNotFoundException(Some(key)))

      context.stop(self)
    case res:HttpResponse =>
    val stream = res.asPartStream()
      stream.foreach { self ! _ }
    case MessageChunk(data, _) =>
      outStream.write(data.toByteArray)
    case ChunkedMessageEnd(_, _) =>
      outStream.flush()
      outStream.close()
      context.stop(self)
    case ChunkedResponseStart(_) =>
      promise.success(Unit)
  }

}

class Deleter(mc: ManagedConnection, promise: Promise[Unit])(implicit system: ActorSystem) extends Actor {

  val io = IO(Http)
  io ! Http.Connect(mc.host, port = mc.port, sslEncryption = mc.isSecure)

  def receive = {
    case Http.Connected(_, _)  =>
      val req = HttpRequest(mc.method, mc.path)
      sender ! req
    case HttpResponse(status, _, _, _) if status.intValue < 400 =>
      promise.success(Unit)
      context.stop(self)
  }

}

class StorageClient(urlStr: String)(implicit system: ActorSystem) extends Storage  {

  import system.dispatcher

  val url = new URL(urlStr)

  def port = url.getPort match {
    case -1 if isSecure => 443
    case -1 if !isSecure => 80
    case x => x
  }

  def isSecure = url.getProtocol == "https"

  def write(key: String, inputStream: InputStream) : Future[Unit] = {
    val promise = Promise[Unit]()
    val mc = ManagedConnection(url.getHost, port, generatePath(s"/${key}/"), POST, isSecure)
    system.actorOf(Props(new Uploader(inputStream, mc, promise)))
    promise.future.map { _ =>
      Unit
    }
  }

  def read(key: String) : Future[InputStream] = {
    val in = new PipedInputStream(2048)
    val out = new PipedOutputStream(in)
    val promise = Promise[Unit]()
    val mc = ManagedConnection(url.getHost, port, generatePath(s"/${key}/"), GET, isSecure)
    system.actorOf(Props(new Downloader(key, out, mc, promise)))
    promise.future.flatMap { _ =>
      Future { in }
    }.recoverWith {
      case ex: ResourceNotFoundException =>
        in.close()
        Future.failed(ex)
    }
  }

  def delete(key: String) : Future[Unit] = {
    val promise = Promise[Unit]()
    val mc = ManagedConnection(url.getHost, port, generatePath(s"/${key}/"), DELETE, isSecure)
    system.actorOf(Props(new Deleter(mc, promise)))
    promise.future
  }

  def generatePath(path: String) = {
    if(urlStr.endsWith("/")) {
      new URL(urlStr.substring(0, urlStr.length() - 1) + path).getPath
    }
    else {
      new URL(urlStr + path).getPath
    }
  }

}