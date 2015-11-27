package kelt.structures.storage.mem

import kelt.structures.storage.{StorageTest, Storage}

import scala.concurrent.ExecutionContext

class MemStorageTest extends StorageTest {

  implicit val ec = ExecutionContext.global

  val storage: Storage = new MemStorage()

}