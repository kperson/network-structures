package kelt.structures.directory

import java.io.{ByteArrayInputStream, OutputStream, InputStream}
import java.net.URL

import akka.actor.ActorSystem
import akka.util.Timeout

import com.codahale.jerkson.Json.parse
import kelt.structures.http._
import kelt.structures.util._

import spray.http._
import scala.concurrent.Future

sealed trait PathContents
case class FileContent(stream: Stream[Array[Byte]]) extends PathContents
case class DirectoryContent(listing: DirectoryListing) extends PathContents


class DirectoryClient(endpoint: String)(implicit system: ActorSystem, timeout: Timeout) extends SprayRequest {

  val basURL = new URL(endpoint)

  import system.dispatcher

  /**
   *
   * @param path
   * @return
   */
  def addFile(path: String) : OutputStream = {
    val url = new URL(basURL, path)
    outStreamForURL(url)
  }

  def delete(path: String) : Future[Unit] = {
    val url = new URL(basURL, path)
    request(HttpRequest(HttpMethods.DELETE, url.toSprayUri)).map(_ => Unit)
  }

  def fetch(path: String) : Future[PathContents] = {
    val url = new URL(basURL, path)
    request(HttpRequest(HttpMethods.GET, url.toSprayUri)).map {
      case r@HttpResponse(status, _, _, _) if r.headers.find(_.name == "X-Type").map(_.value) == Some("File") =>
        FileContent(r.entity.data.toChunkStream(4096).map(_.toByteArray))
      case r@HttpResponse(status, _, _, _) =>
        DirectoryContent(parse[DirectoryListing](r.entity.data.asString))
    }
  }

  def outStreamForURL(url: URL) : OutputStream = {
    new HTTPUploadOutputStream(url, HttpMethods.POST)
  }

}

trait RichDirectoryClient {

  implicit class DirectoryClientExtension(self: DirectoryClient) {

    def addFile(path: String, inputStream: InputStream) : Unit = {
      val stream = self.addFile(path)
      inputStream.stream(4096).foreach { stream.write(_) }
      stream.flush()
      stream.close()
    }

    def addFile(path: String, bytes: Array[Byte]) : Unit = {
      addFile(path, new ByteArrayInputStream(bytes))
    }
  }

}