package com.kelt.structures.directory

import java.io.{ByteArrayInputStream, InputStream}

import scala.concurrent.{ExecutionContext, Future}


sealed trait WriteCommand
case class SaveBytes(bytes: Array[Byte]) extends WriteCommand
case class CloseStorage() extends WriteCommand

case class DirectoryListing(files: List[String], directories: List[String]) {
  def isEmpty = files.isEmpty && directories.isEmpty
}

trait Directory {

  implicit def ec: ExecutionContext

  def name: String

  def directories: Future[List[Directory]]

  def files: Future[List[String]]

  def fileContents(path: List[String]): Future[Option[InputStream]]

  def addFile(fileName: String): Future[(WriteCommand) => Unit]

  def deleteFile(path: List[String]): Future[Unit]

  def deleteDirectory(path: List[String]): Future[Unit]

  def makeDirectory(path: List[String]): Future[Directory]

  def directory(path: List[String]): Future[Option[Directory]] = {
    path match {
      case h :: t =>
        directories.map { q =>
          q.find(x => x.name == h)

        }.flatMap {
          case Some(d) => d.directory(t)
          case _ => Future.successful(None)
        }
      case _ =>
        Future.successful(Some(this))
    }
  }

  def directoryListing: Future[DirectoryListing] = {
    for {
      dirs <- directories
      files <- files
    } yield DirectoryListing(files.sortWith{ (a, b) => a < b }, dirs.map(_.name).sortWith { (a, b) => a < b })
  }


}

sealed trait DirectoryEntry
case class FileItem(item: InputStream) extends DirectoryEntry
case class ChildDirectory(item: Directory) extends DirectoryEntry


trait RichDirectory {

  implicit class DirectoryExtension(self: Directory) {

    import self.ec

    def addFile(fileName: String, bytes: Array[Byte]): Future[Unit] = {
      val is = new ByteArrayInputStream(bytes)
      self.addFile(fileName).map { w =>
        w(SaveBytes(bytes))
        w(CloseStorage())
        Unit
      }
    }

    def directory(path: String): Future[Option[Directory]] = {
      self.directory(path.split("/").filter(_ != "").toList)
    }

    def makeDirectory(path: String): Future[Directory] = {
      self.makeDirectory(path.split("/").filter(_ != "").toList)
    }

    def fileContents(path: String): Future[Option[InputStream]] = {
      self.fileContents(path.split("/").filter(_ != "").toList)
    }

    def item(path: List[String]) : Future[Option[DirectoryEntry]] = {
     self.directory(path).flatMap {
       case Some(dir) => Future.successful(Some(ChildDirectory(dir)))
       case _ => self.fileContents(path).flatMap {
         case Some(f) => Future.successful(Some(FileItem(f)))
         case _ => Future.successful(None)
       }
     }
    }

    def item(path: String) : Future[Option[DirectoryEntry]] = {
      self.item(path.split("/").filter(_ != "").toList)
    }

    def deleteFile(path: String): Future[Unit] = {
      self.deleteFile(path.split("/").filter(_ != "").toList)
    }

    def deleteDirectory(path: String): Future[Unit] = {
      self.deleteDirectory(path.split("/").filter(_ != "").toList)
    }

  }

}