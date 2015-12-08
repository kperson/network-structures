package kelt.structures.lock

import akka.actor.ActorSystem

import java.net.URL

import kelt.structures.http._
import kelt.structures.util._

import scala.concurrent.Future
import scala.concurrent.duration._

import spray.http.{HttpMethods, HttpRequest}



/** A lock client connected over HTTP
 *
 * @param endpoint the base URL for the lock server
 * @param system an actor system used by spray to make service calls
 */
class LockClient(endpoint: String)(implicit system: ActorSystem) extends SprayRequest {

  val basURL = new URL(endpoint)

  import system.dispatcher

  /** Locks a resource
   *
   * @param resource the resource
   * @param acquireTimeout the max time to wait for a lock
   * @param holdTimeout the max time to hold a lock after granted
   * @return a Future when the lock is granted
   */
  def lock(resource: String, acquireTimeout: FiniteDuration, holdTimeout: FiniteDuration): Future[String] = {
    val url = new URL(basURL, s"/${resource}/${acquireTimeout.toMillis}/${holdTimeout.toMillis}/")
    val lockRequest = request(HttpRequest(HttpMethods.POST, url.toSprayUri))
    lockRequest.map(_ => resource).recoverWith {
      case FailedHttpResponse(res) if res.status.intValue == 408 =>
        Future.failed(TimeoutException(acquireTimeout))
    }
  }

  /** Unlocks a resource
   *
   * @param resource the resource
   * @return a Future upon success
   */
  def unlock(resource: String): Future[Unit] = {
    val url = new URL(basURL, s"/${resource}/")
    request(HttpRequest(HttpMethods.DELETE, url.toSprayUri)).map(_ => Unit)
  }

}