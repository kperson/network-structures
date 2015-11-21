package com.kelt.structures.directory.file

import java.io.File

import com.kelt.structures.directory.DirectoryTest
import org.apache.commons.io.FileUtils

import org.scalatest.BeforeAndAfter


class FileDirectoryTest extends DirectoryTest with BeforeAndAfter {

  import scala.concurrent.ExecutionContext.Implicits.global

  val rootName = "root"
  val f = new File(System.getProperty("java.io.tmpdir"), "directorytest")

  if(f.exists()) {
    FileUtils.deleteDirectory(f)
  }


  lazy val directory = new FileDirectory(rootName, f)

}
