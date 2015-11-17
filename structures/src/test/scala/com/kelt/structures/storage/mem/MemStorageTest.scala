package com.kelt.structures.storage.mem

import com.kelt.storage.StorageTest
import com.kelt.structures.storage.{StorageTest, Storage}
import tp.tallyho.storage.{MemStorage, Storage}

import scala.concurrent.ExecutionContext

class MemStorageTest extends StorageTest {

  implicit val ec = ExecutionContext.global

  val storage: Storage = new MemStorage()

}