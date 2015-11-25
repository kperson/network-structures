package com.kelt.structures.storage.file

import java.io.{FileOutputStream, FileInputStream, File, InputStream}

import com.kelt.structures.http.ResourceNotFoundException
import com.kelt.structures.storage.Storage
import com.kelt.structures.util._

import scala.concurrent.Future


class FileStorage(path: File) extends Storage {

  if(!path.exists) {
    path.mkdirs()
  }

  def write(key: String, inputStream: InputStream) : Future[Unit] = {
    val f = new File(path, key)
    val outStream = new FileOutputStream(f)
    val st = inputStream.stream(2048)
    st.takeWhile(_.length != 0).foreach { bytes =>
      outStream.write(bytes)
    }
    outStream.close()
    inputStream.close()
    Future.successful(Unit)
  }

  def read(key: String) : Future[InputStream] = {
    val f = new File(path, key)
    if(f.exists) {
      Future.successful(new FileInputStream(f))
    }
    else {
      Future.failed(ResourceNotFoundException(Some(key)))
    }
  }

  def delete(key: String) : Future[Unit] = {
    val f = new File(path, key)
    if(f.exists) {
      f.delete()
    }
    Future.successful(Unit)
  }
}
