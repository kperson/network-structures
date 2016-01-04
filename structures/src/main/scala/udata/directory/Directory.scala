package udata.directory

import java.io.{OutputStream, InputStream}

import scala.concurrent.{ExecutionContext, Future}


case class DirectoryListing(files: List[String], directories: List[String]) {
  def isEmpty = files.isEmpty && directories.isEmpty
}

trait Directory {


  def directories(path: List[String]) : Future[List[String]]

  def files(path: List[String]) : Future[List[String]]

  def fileContents(path: List[String]): Future[Option[() => InputStream]]

  def addFile(fileName: List[String]): OutputStream

  def delete(path: List[String]): Future[Unit]

  def directoryListing(path: List[String])(implicit ec: ExecutionContext): Future[DirectoryListing] = {
    for {
      dirs <- directories(path)
      files <- files(path)
    } yield DirectoryListing(files.sortWith{ (a, b) => a < b }, dirs.sortWith { (a, b) => a < b })
  }


}

sealed trait DirectoryEntry
case class FileItem(streamFetch: () =>  InputStream) extends DirectoryEntry
case class ChildDirectory(item: DirectoryListing) extends DirectoryEntry


trait RichDirectory {

  implicit class DirectoryExtension(self: Directory) {

    def addFile(fileName: String, bytes: Array[Byte]) {
      val stream = self.addFile(fileName.split("/").filter(_ != "").toList)
      stream.write(bytes)
      stream.close()
    }

    def fileContents(path: String): Future[Option[() => InputStream]] = {
      self.fileContents(path.split("/").filter(_ != "").toList)
    }

    def item(path: List[String])(implicit ec: ExecutionContext) : Future[Option[DirectoryEntry]] = {
      self.fileContents(path).flatMap {
       case Some(is) => Future.successful(Some(FileItem(is)))
       case _ => self.directoryListing(path).flatMap {
         case l if !l.directories.isEmpty || !l.files.isEmpty => Future.successful(Some(ChildDirectory(l)))
         case _ => Future.successful(None)
       }
     }
    }

    def item(path: String)(implicit ec: ExecutionContext) : Future[Option[DirectoryEntry]] = {
      self.item(path.split("/").filter(_ != "").toList)
    }

    def delete(path: String): Future[Unit] = {
      self.delete(path.split("/").filter(_ != "").toList)
    }

  }

}