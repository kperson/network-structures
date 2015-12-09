package kelt.structures.directory

import org.apache.commons.io.IOUtils
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global

trait DirectorySpec extends FlatSpec with Matchers with ScalaFutures {

  def directory: Directory

  "Directory" should "create a directory" in {
    val dir = "d1"
    val makeFetch = directory.makeDirectory(List(dir)).flatMap { _ =>
     directory.directory(dir)
    }

    whenReady(makeFetch, timeout(Span(2, Seconds))) { d =>
      d.get.name should be (dir)
    }
  }


  "Directory" should "create a nested directory" in {
    val dir2 = "d2"
    val dir3 = "d3"
    val makeFetch = directory.makeDirectory(List(dir2, dir3)).flatMap { _ =>
     directory.directory(List(dir2, dir3))
    }

    whenReady(makeFetch, timeout(Span(2, Seconds))) { d =>
      d.get.name should be (dir3)
    }
  }

  "Directory" should "delete a directory" in {
    val dir = "d4"
    val makeDeleteFetch = directory.makeDirectory(List(dir)).flatMap { _ =>
      directory.deleteDirectory(dir)
    }.flatMap { _ =>
      directory.directory(dir)
    }

    whenReady(makeDeleteFetch, timeout(Span(2, Seconds))) { d =>
      d should be(None)
    }

  }

  "Directory" should "create a file" in {
    val contents = "contents"
    val file = "hello.txt"
    val makeFetch = directory.addFile(file, contents.getBytes)
    val fetch = directory.fileContents(file)

    whenReady(fetch, timeout(Span(2, Seconds))) { d =>
      val is = d.get
      val bytes = IOUtils.toByteArray(is)
      is.close()
      new String(bytes) should be(contents)
    }
  }


  "Directory" should "delete a file" in {
    val contents = "contents"
    val file = "hello1.txt"
    directory.addFile(file, contents.getBytes)

    val deleteFetch = directory.deleteFile(file).flatMap { case _ =>
      directory.fileContents(file)
    }

    whenReady(deleteFetch, timeout(Span(2, Seconds))) { is =>
      is should be(None)
    }
  }

}
