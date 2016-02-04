package udata.http

import java.io.{OutputStream, PipedOutputStream, PipedInputStream}
import java.net.URL

import akka.actor.{ActorSystem, Props}
import spray.http.{HttpResponse, HttpHeader, HttpMethods, HttpMethod}

import scala.concurrent.Promise


class HTTPDownloadInputStream(url: URL, method: HttpMethod = HttpMethods.GET, headers: List[HttpHeader] = List.empty)(implicit actorSystem: ActorSystem) extends PipedInputStream(4096) {

  private val promise = Promise[HttpResponse]()

  val outStream = new PipedOutputStream(this)
  val downloader = actorSystem.actorOf(Props(new AsyncDownloader(url, method, headers, outStream, Some(promise))))

  def future = promise.future

}

class HTTPDownloadWriter(url: URL, method: HttpMethod = HttpMethods.GET, outputStream: OutputStream, headers: List[HttpHeader] = List.empty)(implicit actorSystem: ActorSystem)  {

  private val promise = Promise[HttpResponse]()

  val downloader = actorSystem.actorOf(Props(new AsyncDownloader(url, method, headers, outputStream, Some(promise))))

  def future = promise.future

}
