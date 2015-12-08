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

  val baseURL = new URL(if(endpoint.endsWith("/")) endpoint else endpoint + "/")

  import system.dispatcher

  /** increments a count
   *
   * @param resource a count to increment
   * @param count the amount to increment
   * @param ttl the time to live for the increment
   * @param replaceKey if specified, replaces a previous count with the specified key
   * @return a future containing updated count and replace key
   */
  def increment(resource: String, count: Int, ttl: FiniteDuration, replaceKey: Option[String] = None) : Future[UpdateResponse] = {
    val url = replaceKey match {
      case Some(key) => new URL(baseURL, s"${resource}/${count}/${ttl.toMillis}/${key}/")
      case _ => new URL(baseURL, s"${resource}/${count}/${ttl.toMillis}/")
    }
    request(HttpRequest(HttpMethods.POST, url.toSprayUri)).map(r => parse[UpdateResponse](r.entity.data.asString))
  }

  /**
   *
   * @param resource a count to fetch
   * @return a future containing the count
   */
  def count(resource: String) : Future[Int] = {
    val url = new URL(baseURL, s"${resource}/")
    val fetch = request(HttpRequest(HttpMethods.GET, url.toSprayUri)).map(r => parse[ResourceCountResponse](r.entity.data.asString))
    fetch.map(_.count)
  }

}
