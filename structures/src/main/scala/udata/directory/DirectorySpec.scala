package udata.directory

import org.apache.commons.io.IOUtils
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global

trait DirectorySpec extends FlatSpec with Matchers with ScalaFutures {

  def directory: Directory


  "Directory" should "create a file" in {
    val contents = "contents"
    val file = "hello.txt"
    val makeFetch = directory.addFile(file, contents.getBytes)
    val fetch = directory.fileContents(file)

    whenReady(fetch, timeout(Span(2, Seconds))) { d =>
      val is = d.get()
      val bytes = IOUtils.toByteArray(is)
      is.close()
      new String(bytes) should be(contents)
    }
  }


  "Directory" should "delete a file" in {
    val contents = "contents"
    val file = "hello1.txt"
    directory.addFile(file, contents.getBytes)

    val deleteFetch = directory.delete(file).flatMap { case _ =>
      directory.fileContents(file)
    }

    whenReady(deleteFetch, timeout(Span(2, Seconds))) { is =>
      is should be(None)
    }
  }

}
