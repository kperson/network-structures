package com.kelt.structures.directory

import akka.actor.ActorSystem
import akka.util.{ Timeout => AkkaTimeout }

import com.codahale.jerkson.Json._
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
    val c = new directoryclient(endpoint) {
      override def request(req: HttpRequest) : Future[HttpResponse] = {
        Future.successful(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, generate(listing)), headers = List(RawHeader("X-Type", "Directory"))))
      }
    }

    whenReady(c.contents("/hello"), Timeout(Span(2, Seconds))) { x =>
      x should be (DirectoryContent(listing))
    }
  }

  "Directory Client" should "fetch a file" in {

    implicit val timeout = AkkaTimeout(2.seconds)
    import actorSystem.dispatcher

    val str = "hello world"
    val c = new DirectoryClient(endpoint) {
      override def request(req: HttpRequest) : Future[HttpResponse] = {
        Future.successful(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, str), headers = List(RawHeader("X-Type", "File"))))
      }
    }

    val fileContents = c.contents("/hello").map(_.asInstanceOf[FileContent])

    whenReady(fileContents, Timeout(Span(2, Seconds))) { x =>
      val b = new StringBuilder()
      val stream = x.stream
      stream.foreach(r => b.append(new String(r)))
      b.toString should be (str)
    }
  }
}