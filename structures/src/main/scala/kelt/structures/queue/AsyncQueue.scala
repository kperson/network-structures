package kelt.structures.queue

import scala.concurrent.Future

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
   */
  def dequeue(): Future[T]

  /**
   * Closes the queue
   */
  def close()


}