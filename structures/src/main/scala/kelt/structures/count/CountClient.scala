package kelt.structures.count

import akka.actor.ActorSystem

import java.net.URL

import com.codahale.jerkson.Json._

import kelt.structures.http._
import kelt.structures.util._

import spray.http.{HttpMethods, HttpRequest}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration


class CountClient(endpoint: String)(implicit system: ActorSystem) extends SprayRequest {

  val basURL = new URL(endpoint)

  import system.dispatcher

  def increment(resource: String, count: Int, ttl: FiniteDuration, replaceKey: Option[String]) : Future[UpdateResponse] = {
    val url = replaceKey match {
      case Some(key) => new URL(basURL, s"/${resource}/${count}/${ttl.toMillis}/${key}/")
      case _ => new URL(basURL, s"/${resource}/${count}/${ttl.toMillis}/")
    }
    request(HttpRequest(HttpMethods.POST, url.toSprayUri)).map(r => parse[UpdateResponse](r.entity.data.asString))
  }

  def count(resource: String) : Future[Int] = {
    val url = new URL(basURL, s"/${resource}/")
    val fetch = request(HttpRequest(HttpMethods.GET, url.toSprayUri)).map(r => parse[ResourceCountResponse](r.entity.data.asString))
    fetch.map(_.count)
  }

}
