package udata.directory.system

import java.io.File

import udata.directory.DirectorySpec

import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfter

import scala.concurrent.ExecutionContext.Implicits.global

class FileSystemDirectorySpec extends DirectorySpec with BeforeAndAfter {

  behavior of "File System Directory"

  val f = new File(System.getProperty("java.io.tmpdir"), "directorytest")

  if(f.exists()) {
    FileUtils.deleteDirectory(f)
  }

  lazy val directory = new FileSystemDirectory(f)

}
