package kelt.structures.directory

import java.net.URL

import akka.actor.ActorSystem
import akka.util.{Timeout => AkkaTimeout}

import com.codahale.jerkson.Json._
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Matchers, FlatSpec}

import spray.http.HttpHeaders.RawHeader
import spray.http._

import scala.concurrent.duration._
import scala.concurrent.Future


class DirectoryClientTests extends FlatSpec with Matchers with ScalaFutures {

  val endpoint = "http://www.example.com"
  implicit val actorSystem = ActorSystem("directory-client-tests")

  "Directory Client" should "fetch a directory contents" in {

    implicit val timeout = AkkaTimeout(2.seconds)

    val listing = DirectoryListing(List("f1.txt", "f2.txt"), List("dir1", "dir2"))
    val c = new DirectoryClient(endpoint) {
      override def request(req: HttpRequest)(implicit system: ActorSystem) : Future[HttpResponse] = {
        Future.successful(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, generate(listing)), headers = List(RawHeader("X-Type", "Directory"))))
      }
    }

    whenReady(c.fetch("/hello"), Timeout(Span(2, Seconds))) { x =>
      x should be (DirectoryContent(listing))
    }
  }

  "Directory Client" should "fetch a file" in {

    implicit val timeout = AkkaTimeout(2.seconds)
    import actorSystem.dispatcher

    val str = "hello world"
    val c = new DirectoryClient(endpoint) {
      override def request(req: HttpRequest)(implicit system: ActorSystem) : Future[HttpResponse] = {
        Future.successful(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, str), headers = List(RawHeader("X-Type", "File"))))
      }
    }

    val fileContents = c.fetch("/hello").map(_.asInstanceOf[FileContent])

    whenReady(fileContents, Timeout(Span(2, Seconds))) { x =>
      val b = new StringBuilder()
      val stream = x.stream
      stream.foreach(r => b.append(new String(r)))
      b.toString should be (str)
    }
  }

  "Directory Client" should "write to an outputstream" in {
    implicit val timeout = AkkaTimeout(2.seconds)
    val contents = "hello world"
    val out = new ByteOutputStream()
    val c = new DirectoryClient(endpoint) {
      override def outStreamForURL(url: URL) = out
    }
    val stream = c.addFile("/hello")
    stream.write(contents.getBytes)
    val written = out.toString
    written should be (contents)
  }
}