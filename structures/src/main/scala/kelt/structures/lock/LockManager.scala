package kelt.structures.lock

import akka.actor.{DeadLetter, Actor}
import akka.pattern.pipe

import kelt.structures.http.TimeoutException

import scala.concurrent.{Future, Promise}
import scala.collection.mutable.{Map => MutableMap, ListBuffer => MutableList }
import scala.concurrent.duration.FiniteDuration


sealed trait LockAction {

  def lockId: Long = {
    this match {
      case LockAcquire(_, _, i, _) => i
      case LockRelease(_, i) => i
    }
  }

}

case class LockCheck(lockId: Option[Long], resource: String, acquire: Boolean)


sealed trait LockResponse

case class LockGrant(resource: String) extends LockResponse
case class LockTimeout(timeout: FiniteDuration) extends LockResponse


case class LockAcquire(resource: String, promise: Promise[LockResponse], id: Long, holdTimeOut: FiniteDuration) extends LockAction
case class LockRelease(resource: String, id: Long) extends LockAction

case class LockAcquireRequest(resource: String, acquireTimeout: FiniteDuration, holdTimeOut: FiniteDuration)
case class LockReleaseRequest(resource: String, auto: Boolean = false)


class LockManager extends Actor {

  private val nextIds: MutableMap[String, Long] = MutableMap.empty
  private val queues: MutableMap[String, MutableList[(LockAction)]] = MutableMap.empty

  import context.dispatcher

  context.system.eventStream.subscribe(self, classOf[DeadLetter])

  def receive = {
    case LockAcquireRequest(resource, acquireTimeout, holdTimeout) =>
      lock(resource, acquireTimeout, holdTimeout).pipeTo(sender)
    case LockReleaseRequest(resource, auto) => unlock(resource)
    case LockCheck(lockId, resource, acquire) => check(lockId, resource, acquire)
    case DeadLetter(LockGrant(resource), from, to) => self ! LockReleaseRequest(resource, true)
  }


  def lock(resource: String, acquireTimeout: FiniteDuration, holdTimeout: FiniteDuration) : Future[LockResponse] = {
    val promise = Promise[LockResponse]()
    val queue = queueForResource(resource)
    val acquisition = LockAcquire(resource, promise, next(resource), holdTimeout)
    queue.append(acquisition)
    queue.append(LockRelease(resource, acquisition.id))
    self ! LockCheck(Some(acquisition.id), resource, true)
    context.system.scheduler.scheduleOnce(acquireTimeout) {
      if(!promise.isCompleted) {
        promise.success(LockTimeout(acquireTimeout))
        unlock(resource, Some(acquisition.id))
      }
    }
    promise.future
  }



  def unlock(resource: String, mustHaveId: Option[Long] = None) {
    self ! LockCheck(mustHaveId, resource, false)
  }

  def queueForResource(resource: String) : MutableList[LockAction] = {
   queues.get(resource) match {
     case Some(r) => r
     case _ =>
       queues(resource) = MutableList[LockAction]()
       queueForResource(resource)
   }
  }

  def check(lockId: Option[Long], resource: String, acquire: Boolean) {
    val queue = queueForResource(resource)
    queue.headOption.foreach { x =>
      (x, lockId) match {
        case (LockAcquire(r, p, id, holdTimeout), Some(lId)) if lId == id && acquire && r == resource =>
          queue.remove(0)
          if(!p.isCompleted) {
            p.success(LockGrant(resource))
            context.system.scheduler.scheduleOnce(holdTimeout) {
              unlock(resource, Some(id))
            }
          }
          else {
            unlock(resource)
          }
        case (LockRelease(r, id), Some(lId)) if !acquire && r == resource && lId == id =>
          queue.remove(0)
          if(clean(resource)) {
            self ! LockCheck(Some(id + 1), resource, true)
          }
        case (LockRelease(r, id), None) if !acquire && r == resource =>
          queue.remove(0)
          if(clean(resource)) {
            self ! LockCheck(Some(id + 1), resource, true)
          }

        case _ =>
      }
    }
  }

  private def clean(resource: String) : Boolean = {
    val queue = queueForResource(resource)
    if(queue.isEmpty) {
      queues.remove(resource)
      nextIds.remove(resource)
      false
    }
    else {
      true
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
