package com.kelt.structures.directory

import java.io.InputStream
import java.net.URL

import akka.pattern.ask
import akka.actor.{Props, ActorSystem}
import akka.io.IO
import akka.util.Timeout

import com.codahale.jerkson.Json._
import com.kelt.structures.http._
import com.kelt.structures.util._

import com.kelt.structures.storage.Storage._
import spray.can.Http
import spray.http._
import scala.concurrent.Future


sealed trait PathContents
case class FileContent(stream: Stream[Array[Byte]]) extends PathContents
case class DirectoryContent(listing: DirectoryListing) extends PathContents


class DirectoryClient(endpoint: String)(implicit system: ActorSystem, timeout: Timeout) {

  val basURL = new URL(endpoint)

  import system.dispatcher

  def addFile(path: String): (WriteCommand => Unit) = {
    val url = new URL(basURL, path)
    val f = system.actorOf(Props(new AsyncUploader(url)))
    val handler:WriteCommand => Unit = { cmd =>
      f ! cmd
    }
    handler
  }

  def addFile(path: String, inputStream: InputStream) : Unit = {
    val handler = addFile(path)
    inputStream.stream(2048).foreach { x => handler(SaveBytes(x)) }
    handler(CloseStorage)
  }

  def addFile(path: String, str: String) : Unit = {
    val handler = addFile(path)
    handler(SaveBytes(str.getBytes))
    handler(CloseStorage)
  }

  def delete(path: String) : Future[Unit] = {
    val url = new URL(basURL, path)
    request(HttpRequest(HttpMethods.DELETE, url.toSprayUri)).map(_ => Unit)
  }

  def fetch(path: String) : Future[PathContents] = {
    val url = new URL(basURL, path)
    request(HttpRequest(HttpMethods.GET, url.toSprayUri)).map {
      case r@HttpResponse(status, _, _, _) if r.headers.find(_.name == "X-Type").map(_.value) == Some("File") =>
        FileContent(r.entity.data.toChunkStream(2048).map(_.toByteArray))
      case r@HttpResponse(status, _, _, _) =>
        DirectoryContent(parse[DirectoryListing](r.entity.data.asString))
    }
  }

  def request(req: HttpRequest) : Future[HttpResponse] = {

    (IO(Http) ? req).mapTo[HttpResponse].flatMap {
      case r @ HttpResponse(status, _, _, _) if status.intValue < 400 =>
        Future.successful(r)
      case r @ HttpResponse(status, _, _, _) if status.intValue == 404 =>
        Future.failed(ResourceNotFoundException())
      case r @ HttpResponse(_, _, _, _)   =>
        Future.failed(FailedHttpResponse(r))
      case _ => Future.failed(UnknownException())
    }
  }

}
