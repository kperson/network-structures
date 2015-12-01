package kelt.structures.directory.file

import java.io.File

import kelt.structures.directory.DirectoryTest

import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfter

import scala.concurrent.ExecutionContext.Implicits.global

class FileDirectoryTest extends DirectoryTest with BeforeAndAfter {

  val rootName = "root"
  val f = new File(System.getProperty("java.io.tmpdir"), "directorytest")

  if(f.exists()) {
    FileUtils.deleteDirectory(f)
  }

  lazy val directory = new FileDirectory(rootName, f)

}
