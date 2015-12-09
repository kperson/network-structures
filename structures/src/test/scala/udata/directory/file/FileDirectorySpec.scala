package udata.directory.file

import java.io.File

import udata.directory.DirectorySpec

import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfter

import scala.concurrent.ExecutionContext.Implicits.global

class FileDirectorySpec extends DirectorySpec with BeforeAndAfter {

  val rootName = "root"
  val f = new File(System.getProperty("java.io.tmpdir"), "directorytest")

  if(f.exists()) {
    FileUtils.deleteDirectory(f)
  }

  lazy val directory = new FileDirectory(rootName, f)

}
