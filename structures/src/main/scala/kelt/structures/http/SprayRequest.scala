package kelt.structures.http

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import spray.can.Http
import spray.http.{HttpResponse, HttpRequest}

import scala.concurrent.Future


trait SprayRequest {

  def request(req: HttpRequest)(implicit system: ActorSystem, timeout: Timeout) : Future[HttpResponse] = {

    import system.dispatcher

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
