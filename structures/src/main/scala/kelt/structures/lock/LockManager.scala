package kelt.structures.lock

import akka.actor.Actor
import akka.pattern.pipe

import kelt.structures.http.TimeoutException

import scala.concurrent.{Future, Promise}
import scala.collection.mutable.{Map => MutableMap, ListBuffer => MutableList }
import scala.concurrent.duration.FiniteDuration


sealed trait LockAction {

  def lockId: Long = {
    this match {
      case LockAcquire(_, _, i) => i
      case LockRelease(_, i) => i
    }
  }

}

case class LockAcquire(resource: String, promise: Promise[Long], id: Long) extends LockAction
case class LockRelease(resource: String, id: Long) extends LockAction

case class LockAcquireRequest(resource: String, timeout: FiniteDuration)
case class LockReleaseRequest(resource: String, id: Long)


class LockManager extends Actor {

  private val nextIds: MutableMap[String, Long] = MutableMap.empty
  private val queues: MutableMap[String, MutableList[(LockAction)]] = MutableMap.empty

  import context.dispatcher

  def receive = {
    case LockAcquireRequest(resource, timeout) => lock(resource, timeout).pipeTo(sender)
    case LockReleaseRequest(resource, id) => unlock(id, resource)
  }

  def lock(resource: String, timeout: FiniteDuration) : Future[Long] = {
    val promise = Promise[Long]()
    val queue = queueForResource(resource)
    val acquisition = LockAcquire(resource, promise, next(resource))
    queue.append(acquisition)
    queue.append(LockRelease(resource, acquisition.id))
    check(acquisition.id, resource, true)
    context.system.scheduler.scheduleOnce(timeout) {
      if(!promise.isCompleted) {
        promise.failure(TimeoutException(timeout))
        self ! LockReleaseRequest(resource, acquisition.id)
      }
    }
    promise.future
  }

  def unlock(lockId: Long, resource: String) {
    check(lockId, resource, false)
    val queue = queueForResource(resource)
    queues(resource) = queue.filter(_.lockId == lockId)
  }

  def queueForResource(resource: String) : MutableList[LockAction] = {
   queues.get(resource) match {
     case Some(r) => r
     case _ =>
       queues(resource) = MutableList[LockAction]()
       queueForResource(resource)
   }
  }

  def check(lockId: Long, resource: String, acquire: Boolean) {
    val queue = queueForResource(resource)
    queue.headOption.foreach {
      case a @ LockAcquire(r, p, id) if lockId == id && acquire && r == resource  =>
        queue.remove(0)
        clean(resource)
        p.success(lockId)
      case a @ LockRelease(r, id) if lockId == id && !acquire && r == resource =>
        queue.remove(0)
        clean(resource)
        check(lockId + 1, resource, true)
      case _ =>
    }
  }

  private def clean(resource: String) {
    val queue = queueForResource(resource)
    if(queue.isEmpty) {
      println("clean")
      queues.remove(resource)
      nextIds.remove(resource)
    }
  }

  private def next(resource: String) : Long = {
    nextIds.get(resource) match {
      case Some(s) =>
        nextIds(resource) = s + 1
        s
      case _ =>
        nextIds(resource) = Long.MinValue
        next(resource)
    }
  }
}
