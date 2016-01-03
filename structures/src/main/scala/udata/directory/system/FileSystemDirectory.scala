package udata.directory.system

import java.io._

import org.apache.commons.io.FileUtils

import scala.concurrent.{ExecutionContext, Future}

import udata.directory.Directory





class FileSystemDirectory(rootPath: File) extends Directory {

  rootPath.mkdirs()

  def files(path: List[String]) : Future[List[String]] = {
    val f = new File(rootPath, path.mkString("/"))
    if(f.exists() && f.isDirectory) {
      Future.successful(f.listFiles().filter(_.isFile).map(_.getName).toList)
    }
    else {
      Future.successful(List.empty)
    }
  }


  def directories(path: List[String]) : Future[List[String]] = {
    val f = new File(rootPath, path.mkString("/"))
    if(f.exists() && f.isDirectory) {
      val dirs = f.listFiles().filter(_.isDirectory).map(x => x.getName).toList
      Future.successful(dirs)
    }
    else {
      Future.successful(List.empty)
    }
  }

  def fileContents(path: List[String]) : Future[Option[() => InputStream]] = {
    val f = new File(rootPath, path.mkString("/"))
    if(f.exists && f.isFile) {
      Future.successful(Some(() => new FileInputStream(f)))
    }
    else {
      Future.successful(None)
    }
  }

  def addFile(fileName: List[String]): OutputStream = {
    val f = new File(rootPath, fileName.mkString("/"))
    f.getParentFile.mkdirs()
    new FileOutputStream(f)
  }

  def delete(path: List[String]) : Future[Unit] = {
    val f = new File(rootPath, path.mkString("/"))
    if(f.exists && f.isFile) {
      f.delete()
    }
    else if(f.exists && f.isDirectory) {
      if(f == rootPath) {
        f.listFiles().filter(_.isDirectory).foreach(FileUtils.deleteDirectory(_))
        f.listFiles().filter(_.isFile).foreach(_.delete())
      }
      else {
        FileUtils.deleteDirectory(f)
      }
    }
    Future.successful(Unit)
  }

}