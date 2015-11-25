package com.kelt.structures.http

import akka.actor.{Props, ActorSystem}

import java.io.OutputStream
import java.net.URL

import spray.http.{HttpHeader, HttpMethods, HttpMethod}


class HTTPUploadOutputStream(url: URL, method: HttpMethod = HttpMethods.POST, headers: List[HttpHeader] = List.empty)(implicit actorSystem: ActorSystem) extends OutputStream {

  val uploader = actorSystem.actorOf(Props(new AsyncUploader(url, method)))
  private var isCompleted = false

  def write(b: Int) {
    val lower8 = b & 0xFF
    uploader ! SaveBytes(Array(lower8.toByte))
  }

  override def write(bytes: Array[Byte]) {
    uploader ! SaveBytes(bytes)
  }

  override def write(b: Array[Byte], offset: Int, length: Int) {
    write(b.drop(offset).take(length))
  }

  override def close() {
    complete()
  }

  override def flush() {
    complete()
  }

  private def complete() {
    if(!isCompleted) {
      uploader ! CloseStorage
      isCompleted = true
    }
  }

}
