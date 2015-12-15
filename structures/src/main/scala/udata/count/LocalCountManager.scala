package udata.count

import java.util.UUID

import akka.actor.{Cancellable, Actor}

import scala.collection.mutable
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration.FiniteDuration

case class CountEntry private[count] (amount: Int, cancellable: Cancellable) {

  def isCancelled = cancellable.isCancelled

  def cancel() {
    if(!isCancelled) {
      cancellable.cancel()
    }
  }

}

case class AutoDecrementRequest private[count] (resourceKey: String, replaceKey: String)

object CountManager {
  case class UpdateCountRequest(resourceKey: String, count: Int, ttl: FiniteDuration, replaceKey: Option[String] = None)
  case class UpdateResponse(resourceKey: String, replaceKey: String, count: Int)

  case class ResourceCountRequest(resourceKey: String)
  case class ResourceCountResponse(count: Int)
}

class LocalCountManager extends Actor {

  import CountManager._

  def receive = {
    case UpdateCountRequest(resourceKey, amount, ttl, replaceKey) => sender ! update(resourceKey, amount, ttl, replaceKey)
    case AutoDecrementRequest(resourceKey, replaceKey) => autoDecrement(resourceKey, replaceKey)
    case ResourceCountRequest(resourceKey) => sender ! ResourceCountResponse(countForResource(resourceKey))
  }

  private val counts = MutableMap[String, (Int, MutableMap[String, CountEntry])]()

  private def update(resourceKey: String, amount: Int, ttl: FiniteDuration, replaceKey: Option[String]): UpdateResponse = {
    import context.dispatcher
    val newId = UUID.randomUUID().toString
    val (currentCt, countRef) = count(resourceKey)
    val newCt = replaceKey match {
      case Some(k)if countRef.get(k) != None =>
        val entry = countRef.remove(k).get
        entry.cancel()
        currentCt - entry.amount + amount
      case _ =>
        currentCt + amount
    }

    val schedule = context.system.scheduler.scheduleOnce(ttl) {
      self ! AutoDecrementRequest(resourceKey, newId)
    }
    val newEntry = CountEntry(amount, schedule)
    countRef(newId) = newEntry
    counts(resourceKey) = (newCt, countRef)
    UpdateResponse(resourceKey, newId, newCt)
  }

  private def countForResource(resourceKey: String) = counts.get(resourceKey) match {
    case Some(e) => e._1
    case _ => 0
  }

  private def count(resourceKey: String): (Int, MutableMap[String, CountEntry]) = {
    counts.get(resourceKey) match {
      case Some(e) => e
      case _ =>
        counts(resourceKey) = (0, mutable.Map.empty)
        count(resourceKey)
    }
  }

  private def autoDecrement(resourceKey: String, replaceKey: String) {
    val (currAmount, countRef) = count(resourceKey)
    countRef.remove(replaceKey).foreach { e =>
      if(!e.isCancelled) {
        if (countRef.isEmpty) {
          counts.remove(resourceKey)
        }
        else {
          counts(resourceKey) = (currAmount - e.amount, countRef)
        }
      }
    }
  }

}
