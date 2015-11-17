package com.kelt.structures.storage.mem

import java.io.{ByteArrayInputStream, InputStream}

import com.kelt.structures.storage.{Storage, StorageNotFoundException}
import org.apache.commons.io.IOUtils

import scala.concurrent.Future


class MemStorage extends Storage {

  private var cache:scala.collection.mutable.Map[String, Array[Byte]] = scala.collection.mutable.HashMap[String, Array[Byte]]()

  def write(key: String, inputStream: InputStream) : Future[Unit] = {
    val bytes = IOUtils.toByteArray(inputStream)
    cache += (key -> bytes)
    Future.successful(Unit)
  }

  def read(key: String) : Future[InputStream] = {
    cache.get(key).map(x => Future.successful(new ByteArrayInputStream(x))).getOrElse(Future.failed(StorageNotFoundException(key)))
  }

  def delete(key: String) : Future[Unit] = {
    cache -= key
    Future.successful(Unit)
  }
}
