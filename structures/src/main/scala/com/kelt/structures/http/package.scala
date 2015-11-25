package com.kelt.structures

import com.kelt.structures.directory.RichDirectoryClient
import spray.http.HttpResponse


package object http extends RichDirectoryClient {

  case object SendTrigger

  sealed trait WriteCommand
  case class SaveBytes(bytes: Array[Byte]) extends WriteCommand
  case object CloseStorage extends WriteCommand

  case class ResourceNotFoundException(id: Option[String] = None) extends RuntimeException("resource not found")
  case class UnknownException() extends Exception

  case class FailedHttpResponse(response: HttpResponse) extends RuntimeException(s"response failed with status code ${response.status.intValue}")

}