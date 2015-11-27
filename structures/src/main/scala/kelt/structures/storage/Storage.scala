package kelt.structures.storage

import java.io._

import kelt.structures.util._

import scala.concurrent.{ExecutionContext, Future}


trait Storage {

  /**
   *
   * @param key the key in the storage system
   * @return a future completed when the stream is saved
   */
  def write(key: String, inputStream: InputStream) : Future[Unit]

  /**
   *
   * @param key the key for the file to fetch
   * @return a future containing the input stream or None when (if no data is present)
   */
  def read(key: String) : Future[InputStream]

  /**
   *
   * @param key the key for the data to delete
   * @return a future completed completed when then file is deleted
   */
  def delete(key: String) : Future[Unit]

}

object Storage {

  implicit class StorageExtensions(self: Storage) {

    def writeDataFromFile(key: String, file: File) = {
      self.write(key, new FileInputStream(file))
    }

    def writeToStream(key: String, outStream: OutputStream, bufferSize: Int = 4096)(implicit ec: ExecutionContext) = {
      self.read(key).map { is =>

        val st = is.stream(bufferSize)
        st.takeWhile(_.length != 0).foreach { bytes =>
          outStream.write(bytes)
        }
        outStream.close()
        is.close()
        Future.successful(Unit)
      }
    }


    def writeToFile(key: String, fileName: String, bufferSize: Int = 4096)(implicit ec: ExecutionContext) = {
      writeToStream(key, new FileOutputStream(fileName), bufferSize)
    }
  }

}
