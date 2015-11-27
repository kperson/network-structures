package kelt.structures.storage

import java.io.{FileInputStream, PrintWriter, File}

import kelt.structures.http.ResourceNotFoundException

import org.apache.commons.io.IOUtils
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Span, Seconds}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext


trait StorageTest extends FlatSpec with Matchers with ScalaFutures {

  def storage: Storage
  implicit val ec: ExecutionContext

  val storageKey = s"storageKey-${System.currentTimeMillis}"
  val file = File.createTempFile("temp", ".txt")
  val fileContents = "file contents"

    new PrintWriter(file.getAbsolutePath) {
      write(fileContents); close
    }



  "Storage" should "retrieve nothing when data has not been set" in {
    val storageGet = storage.read(s"${storageKey}1") map { is => IOUtils.toString(is, "UTF-8") }

    whenReady(storageGet.failed, timeout(Span(15, Seconds))) { ex =>
      ex shouldBe a [ResourceNotFoundException]
    }
  }

  "Storage" should "save to storage" in {
    val setAndGet = storage.write(storageKey, new FileInputStream(file)) flatMap { _ =>
      storage.read(storageKey) map { is =>
        IOUtils.toString(is, "UTF-8")
      }
    }

    whenReady(setAndGet, timeout(Span(15, Seconds))) { rs =>
      rs should be (fileContents)
    }
  }

  "Storage" should "delete from storage" in {
    val setDeleteGet = storage.write(storageKey, new FileInputStream(file)) flatMap { _ =>
      storage.read(storageKey) map { is =>
        IOUtils.toString(is, "UTF-8")
      }
    } flatMap { _ =>
      storage.delete(storageKey)
    } flatMap { _ =>
      storage.read(storageKey) map { is => IOUtils.toString(is, "UTF-8") }
    }

    whenReady(setDeleteGet.failed, timeout(Span(15, Seconds))) { ex =>
      ex shouldBe a [ResourceNotFoundException]
    }
  }

}