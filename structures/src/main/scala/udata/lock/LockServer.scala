package udata.lock

import akka.actor.ActorRef
import akka.pattern.ask

import spray.http.HttpResponse

import scala.concurrent.Future
import scala.concurrent.duration._

import udata.server.BasicSprayServer
import udata.lock.LockManager._


trait LockServer extends BasicSprayServer {

  def lockManager: ActorRef

  import context.dispatcher

  get("/lock/:resource/:acquireTimeout/:holdTimeout/?") { (params, client) =>
    val resource = params("resource").head
    val acquireTimeout = params("acquireTimeout").head.toLong.milliseconds
    val holdTimeout = params("holdTimeout").head.toLong.milliseconds
    val fetch = (lockManager ? LockAcquireRequest(resource, acquireTimeout, holdTimeout))(acquireTimeout + 3.seconds).asInstanceOf[Future[LockAcquireResponse]]

    fetch onSuccess {
      case _: LockGrant => client ! HttpResponse(status = 200)
      case _: LockTimeout => client ! HttpResponse(status = 403)
    }

    fetch onFailure {
      case _ => client ! HttpResponse(status = 500)
    }
  }

  delete("/lock/:resource/?") { (params, client, body) =>
    val resource = params("resource").head
    lockManager ! LockReleaseRequest(resource)
    client ! HttpResponse(status = 202)
  }
}
