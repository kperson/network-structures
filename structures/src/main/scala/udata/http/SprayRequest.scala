package udata.http

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import spray.can.Http
import spray.http.{HttpResponse, HttpRequest}

import scala.concurrent.Future
import scala.concurrent.duration._


trait SprayRequest {

  def request(req: HttpRequest)(implicit system: ActorSystem) : Future[HttpResponse] = {

    import system.dispatcher

    //we don't want the ask to timeout, the HTTP connection can timeout if necessary
    implicit val timeout:Timeout = 200.days

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
