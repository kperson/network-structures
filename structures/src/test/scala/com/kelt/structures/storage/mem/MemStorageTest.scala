package com.kelt.structures.storage.mem

import com.kelt.structures.storage.{StorageTest, Storage}

import scala.concurrent.ExecutionContext

class MemStorageTest extends StorageTest {

  implicit val ec = ExecutionContext.global

  val storage: Storage = new MemStorage()

}