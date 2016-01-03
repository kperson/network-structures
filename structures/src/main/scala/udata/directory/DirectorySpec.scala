package udata.directory

import org.apache.commons.io.IOUtils

import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import udata.util.TestUtils._


trait DirectorySpec extends FlatSpec with Matchers with ScalaFutures {

  def directory: Directory


  it should "create a file" in {
    val contents = "contents"
    val file = "hello.txt"
    directory.addFile(file, contents.getBytes)
    val fetch = directory.fileContents(file)

    whenReady(fetch, 3.seconds) { d =>
      val is = d.get()
      val bytes = IOUtils.toByteArray(is)
      is.close()
      new String(bytes) should be(contents)
    }
  }


  it should "delete a file" in {
    val contents = "contents"
    val file = "hello1.txt"
    directory.addFile(file, contents.getBytes)

    val deleteFetch = directory.delete(file).flatMap { case _ =>
      directory.fileContents(file)
    }

    whenReady(deleteFetch, 3.seconds) { is =>
      is should be(None)
    }
  }

  it should "add nested files" in {
    val contents = "contents"
    val fileName = "hello.text"
    val fileParent = List("hello", "world")
    val filePath = fileParent ++ List(fileName)

    val outStream = directory.addFile(filePath)
    outStream.write(contents.getBytes)
    outStream.close()

    val fileFetch = directory.fileContents(filePath).map {
      case Some(a) => Some(a())
      case _ => None
    }

    whenReady(fileFetch, 4.seconds) { is =>
      is shouldBe a [Some[_]]
      val stream = is.get
      val bytes = IOUtils.toByteArray(stream)
      new String(bytes) should be (contents)
    }
  }

  it should "add read nested directories" in {
    val contents = "contents"
    val fileName = "hello.text"
    val fileParent = List("hello", "world")
    val filePath = fileParent ++ List(fileName)

    val outStream = directory.addFile(filePath)
    outStream.write(contents.getBytes)
    outStream.close()

    val outStream2 = directory.addFile(List(fileParent.head) ++ List(fileName))
    outStream2.write(contents.getBytes)
    outStream2.close()

    val fileListing = directory.files(fileParent)
    val directoryListing = directory.directories(List(fileParent.head))
    val fileAndDirectoryListing = directory.directoryListing(List(fileParent.head))


    whenReady(fileListing, 4.seconds) { ls =>
      ls should be (List(fileName))
    }

    whenReady(directoryListing, 4.seconds) { ls =>
      ls should be (List(fileParent(1)))
    }

    whenReady(fileAndDirectoryListing, 4.seconds) { ls =>
      ls should be (DirectoryListing(List(fileName), List(fileParent(1))))
    }

  }


}
