package com.kelt.structures

import spray.http.HttpResponse

/**
 * Created by keltonperson on 11/24/15.
 */
package object http {

  case object SendTrigger

  sealed trait WriteCommand
  case class SaveBytes(bytes: Array[Byte]) extends WriteCommand
  case object CloseStorage extends WriteCommand

  case class ResourceNotFoundException() extends RuntimeException("resource not found")
  case class UnknownException() extends Exception

  case class FailedHttpResponse(response: HttpResponse) extends RuntimeException(s"response failed with status code ${response.status.intValue}")

}
