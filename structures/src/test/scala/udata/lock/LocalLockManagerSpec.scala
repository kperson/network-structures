package udata.lock

import akka.actor.{ActorSystem, Props}


class LocalLockManagerSpec extends LockManagerSpec {

  def lockManager(system: ActorSystem) = system.actorOf(Props(new LocalLockManager))

}