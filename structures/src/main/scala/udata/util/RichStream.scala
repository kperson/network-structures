package udata.util

import java.io.{OutputStream, InputStream}

trait RichStream {

  implicit class InputStreamExtension(self: InputStream) {

    def pipeToOutputStream(outputStream: OutputStream) = {
      self.stream(4096).foreach(outputStream.write(_))
    }

    def stream(bufferSize: Int) : Stream[Array[Byte]] = {
      var buffer = new Array[Byte](bufferSize)
      val stream = Stream.continually(self.read(buffer))
        .takeWhile { x =>
        if(x == -1) {
          buffer = null
          false
        }
        else {
          true
        }
      }.map {
        buffer.take(_)
      }
      stream
    }
  }

}
