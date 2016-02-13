package udata

import spray.http.HttpResponse

import scala.concurrent.duration.FiniteDuration


package object http {

  //really, really, really bad back to deal with S3's like of streaming
  val terminatingStr = "5HUwroufTEmx6kietNtQ4xDRwoe7KmVuxGH9gu0dhVEFeHB3HZg6zHZrTDahNM9zdueH3lRToNBGpiRV"

  case object SendTrigger
  case object CloseConclusion

  sealed trait WriteCommand
  case class SaveBytes(bytes: Array[Byte]) extends WriteCommand
  case object CloseStorage extends WriteCommand

  case class ResourceNotFoundException(id: Option[String] = None) extends RuntimeException("resource not found")
  case class UnknownException() extends Exception

  case class TimeoutException(duration: FiniteDuration) extends RuntimeException(s"timed out in ${duration.toMillis}")

  case class FailedHttpResponse(response: HttpResponse) extends RuntimeException(s"response failed with status code ${response.status.intValue}")

}