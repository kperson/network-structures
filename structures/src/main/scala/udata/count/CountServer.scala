package udata.count

import akka.actor.ActorRef
import akka.pattern.ask

import com.codahale.jerkson.Json.generate

import spray.http.{ContentTypes, HttpEntity, HttpResponse}

import scala.concurrent.Future
import scala.concurrent.duration._

import udata.count.CountManager._
import udata.server._


trait CountServer extends BasicSprayServer {

  def countManager: ActorRef

  import context.dispatcher

  post("/count/:resourceKey/:amount/:ttl/:replaceKey/?") { (params, client, body) =>
    val resource = params("resourceKey").head
    val amount = params("amount").head.toInt
    val ttl = params("ttl").head.toLong.milliseconds
    val replaceKey = params("replaceKey").head
    updateCount(client, resource, amount, ttl, Some(replaceKey))
  }

  post("/count/:resourceKey/:amount/:ttl/?") { (params, client, body) =>
    val resource = params("resourceKey").head
    val amount = params("amount").head.toInt
    val ttl = params("ttl").head.toLong.milliseconds
    updateCount(client, resource, amount, ttl)
  }

  get("/count/:resourceKey/?") { (params, client) =>
    val resource = params("resourceKey").head
    val askTimeout: FiniteDuration = 10.seconds
    val fetch = (countManager ? ResourceCountRequest(resource))(askTimeout).asInstanceOf[Future[ResourceCountResponse]]
    fetch.onSuccess { case x =>
      val entity = HttpEntity(ContentTypes.`application/json`, generate(x))
      client ! HttpResponse(status = 200, entity = entity)
    }
  }

  private def updateCount(client: ActorRef, resource: String, amount: Int, ttl: FiniteDuration, replaceKey: Option[String] = None) {
    val askTimeout: FiniteDuration = 10.seconds
    val fetch = (countManager ? UpdateCountRequest(resource, amount, ttl, replaceKey))(askTimeout).asInstanceOf[Future[UpdateResponse]]
    fetch.onSuccess { case x =>
      val entity = HttpEntity(ContentTypes.`application/json`, generate(x))
      client ! HttpResponse(status = 200, entity = entity)
    }
  }

}