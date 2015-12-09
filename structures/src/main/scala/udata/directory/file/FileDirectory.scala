package udata.directory.file

import java.io._

import org.apache.commons.io.FileUtils

import scala.concurrent.{ExecutionContext, Future}

import udata.directory.Directory


case class FileDirectory(val name: String, rootPath: File, parent: Option[String] = None)(implicit val ec: ExecutionContext) extends Directory {

  rootPath.mkdirs()

  def files: Future[List[String]] = {
    val f = new File(rootPath, relativePath)
    Future.successful(f.listFiles().filter(_.isFile).map(_.getName).toList)
  }

  def makeDirectory(path: List[String]) : Future[Directory] = {
    path match {
      case h::t =>
        directory(List(h)) flatMap {
          case Some(x) => x.makeDirectory(t)
          case _ =>
            val f = new File(rootPath, relativePath)
            val dirToMake = new File(f, h)
            dirToMake.mkdirs()
            val dir = new FileDirectory(h, rootPath, Some(relativePath))
            dir.makeDirectory(t)
        }
      case _ => Future.successful(this)
    }
  }

  def directories: Future[List[Directory]] = {
    val f = new File(rootPath, relativePath)
    if(f.exists()) {
      Future.successful(f.listFiles().filter(_.isDirectory).map(x => FileDirectory(x.getName, rootPath, Some(relativePath))).toList)
    }
    else {
      Future.successful(List.empty)
    }
  }

  def fileContents(path: List[String]) : Future[Option[InputStream]] = {
    val f = new File(new File(rootPath, relativePath), path.mkString("/"))
    if(f.exists && f.isFile) {
      Future.successful(Some(new FileInputStream(f)))
    }
    else {
      Future.successful(None)
    }
  }

  def addFile(fileName: String): OutputStream = {
    val f = new File(new File(rootPath, relativePath), fileName)
    new FileOutputStream(f)
  }

  def deleteFile(path: List[String]) : Future[Unit] = {
    val f = new File(new File(rootPath, relativePath), path.mkString("/"))
    if(f.exists && f.isFile) {
      f.delete()
      if(path.length > 1) {
        val parentPath = path.take(path.length - 1)
        directory(parentPath).flatMap {
          case Some(a) => a.directoryListing.flatMap { x =>
            if (x.isEmpty) {
              deleteDirectory(parentPath).map(_ => Unit)
            }
            else {
              Future.successful(Unit)
            }
          }
          case _ => Future.successful(Unit)
        }
      }
      else {
        Future.successful(Unit)
      }
    }
    else {
      Future.successful(Unit)
    }
  }

  def deleteDirectory(path: List[String]): Future[Unit] = {
    val f = new File(new File(rootPath, relativePath), path.mkString("/"))
    if(f.exists && f.isDirectory) {
      FileUtils.deleteDirectory(f)
      if(path.length > 1) {
        val parentPath = path.take(path.length - 1)
        directory(parentPath).flatMap {
          case Some(a) => a.directoryListing.flatMap { x =>
            if (x.isEmpty) {
              deleteDirectory(parentPath).map(_ => Unit)
            }
            else {
              Future.successful(Unit)
            }
          }
          case _ => Future.successful(Unit)
        }
      }
      else {
        Future.successful(Unit)
      }
    }
    else {
      Future.successful(Unit)
    }
  }



  private def relativePath: String = {
    parent match {
      case Some(p) => new File(p, name).getAbsolutePath
      case _ => ""
    }
  }

}