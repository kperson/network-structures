package com.kelt.structures.pubsub

import scala.collection.mutable.ListBuffer


trait PubSub[T] {

  private var open: Boolean = true
  private val handlers:ListBuffer[T => Unit]  = ListBuffer()
  private var filterCheck:T => Boolean = { x => true }

  def channel: String

  def publish(data: T)

  /**
   * Process incoming data
   *
   * @param data the data to be processed
   */
  def processIncoming(data: T) = {
    if(isOpen && filterCheck(data)) {
      handlers.foreach { h =>
        h(data)
      }
    }
  }

  /**
   * Registers a callback for pub sub channel
   *
   * @param handler a callback to execute each time date is delivered
   */
  def foreach(handler: T => Unit) = {
    handlers.append(handler)
  }

  /**
   * Filters out  data from the pub sub channel
   *
   * @param f a filter clause
   */
  def filter(f: T => Boolean) = {
    filterCheck = f
  }

  /**
   * Maps the pub sub to a pipe
   *
   * @param handler map function
   * @tparam B the transformed type of the pipe
   * @return a configure pipe
   */
  def map[B](handler: T => B) : Pipe[T, B] = {
    val p = new Pipe[T, B](handler)
    foreach { t =>
      p.processIncoming(t)
    }
    p
  }

  /**
   * Determines if a pub sub subscription is open
   *
   * @return the open status
   */
  def isOpen = open

  /**
   * Closes the pub sub subscription
   */
  def close() {
    open = false
  }

}

class LocalPubSub[T](val channel: String, manager: PubSubManager[T]) extends PubSub[T] {

  val listenerId = manager.addListener(channel) { data =>
    processIncoming(data.payload)
  }

  override def publish(data: T) {
    manager.save(channel, data)
  }

  override def close() {
    super.close()
    manager.removeListener(channel, listenerId)
  }

}


class Pipe[T, B](transform:(T) => B) {

  private val handlers:ListBuffer[B => Unit] = ListBuffer()

  def processIncoming(data: T) = {
    val g = transform(data)
    handlers.foreach { x =>
      x(g)
    }
  }

  def foreach(handler: B => Unit) = {
    handlers.append(handler)
  }

  def map[Q](t:(B) => Q):Pipe[B, Q] = {
    val p = new Pipe[B, Q](t)
    handlers.append { x =>
      p.processIncoming(x)
    }
    p
  }

}
