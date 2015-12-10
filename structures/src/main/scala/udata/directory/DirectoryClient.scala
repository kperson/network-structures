package udata.directory

import akka.actor.ActorSystem

import com.codahale.jerkson.Json.parse

import java.io.{ByteArrayInputStream, OutputStream, InputStream}
import java.net.URL

import scala.concurrent.Future

import spray.http._

import udata.http._
import udata.util._


sealed trait PathContents
case class FileContent(inputStream: InputStream) extends PathContents
case class DirectoryContent(listing: DirectoryListing) extends PathContents


/** an HTTP file storage client
 *
 * @param endpoint the HTTP endpoint of the storage server
 * @param system the actor system to make spray HTTP requests
 */
class DirectoryClient(endpoint: String)(implicit system: ActorSystem) extends SprayRequest {

  val baseURL = new URL(if(endpoint.endsWith("/")) endpoint else endpoint + "/")

  import system.dispatcher

  /** adds a file
   *
   * @param path file path
   * @return an outstream to store data
   */
  def addFile(path: String) : OutputStream = {
    val url = new URL(baseURL, cleanPath(path))
    outStreamForURL(url)
  }

  /** deletes a file or directory
   *
   * @param path path of the resource
   * @return a future when completed
   */
  def delete(path: String) : Future[Unit] = {
    val url = new URL(baseURL, cleanPath(path))
    request(HttpRequest(HttpMethods.DELETE, url.toSprayUri)).map(_ => Unit)
  }

  /** fetches a file or list directory
   *
   * @param path of file of directory
   * @return path contents enum
   */
  def fetch(path: String) : Future[PathContents] = {
    val url = new URL(baseURL, cleanPath(path))
    val inputStream = new HTTPDownloadInputStream(url)
    val res = inputStream.future.map {
      case r@HttpResponse(status, _, _, _) if r.headers.find(_.name == "X-Type").map(_.value) == Some("File") =>
        FileContent(inputStream)
      case r@HttpResponse(status, _, _, _) =>
        DirectoryContent(parse[DirectoryListing](inputStream))
    }

    res.recoverWith {
      case FailedHttpResponse(r) if r.status.intValue == 404 => Future.failed(ResourceNotFoundException())
    }
  }

  private def cleanPath(path: String) = {
    if(path.startsWith("/")) {
      path.substring(1, path.length)
    }
    else {
      path
    }
  }

  private def outStreamForURL(url: URL) : OutputStream = {
    new HTTPUploadOutputStream(url, HttpMethods.POST)
  }

}

trait RichDirectoryClient {

  implicit class DirectoryClientExtension(self: DirectoryClient) {

    /** adds a file from an input stream
     *
     * @param path file path
     * @param inputStream input to upload
     */
    def addFile(path: String, inputStream: InputStream) : Unit = {
      val stream = self.addFile(path)
      inputStream.stream(4096).foreach { stream.write(_) }
      stream.flush()
      stream.close()
    }

    /** adds a file from an byte array
     *
     * @param path file path
     * @param bytes data
     */
    def addFile(path: String, bytes: Array[Byte]) : Unit = {
      addFile(path, new ByteArrayInputStream(bytes))
    }
  }

}