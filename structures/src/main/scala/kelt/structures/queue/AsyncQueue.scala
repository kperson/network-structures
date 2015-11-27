package kelt.structures.queue

trait AsyncQueue[T] {

  /**
   * Adds date to the queue
   *
   * @param t date to add
   */
  def enqueue(t: T)

  /**
   * Register to have item pulled from the queue
   * You must dequeue again once the handle is executed to process more data
   * Handler is only executed once
   *
   * @param handler a callback to execute once a item is available for processing
   */
  def dequeue(handler: T => Unit)

  /**
   * Closes the queue
   */


  def map[S](handler: T => S) : AsyncQueue[S] = {
    val piped = new PipedQueue[S]()
    dequeue { data =>
      piped.enqueue(handler(data))
    }
    return piped
  }

  def close()


}

class PipedQueue[T] extends AsyncQueue[T] {

  private var internalHandler: T => Unit = { x =>  }

  def enqueue(t: T): Unit = {
    internalHandler(t)
  }

  def dequeue(handler: T => Unit): Unit = {
    internalHandler = handler
  }

  def close() { }

}
