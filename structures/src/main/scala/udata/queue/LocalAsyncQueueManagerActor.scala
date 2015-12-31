package udata.queue


class LocalAsyncQueueManagerActor extends AsyncQueueManagerActor {

  lazy val manager = new AsyncQueueManager[Array[Byte]]()

}
