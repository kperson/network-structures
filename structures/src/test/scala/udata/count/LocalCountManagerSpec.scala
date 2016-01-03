package udata.count

import akka.actor.{ActorRef, ActorSystem, Props}


class LocalCountManagerSpec extends CountManagerSpec {

  def displayName = "Local Count Manager"

  def countManager(system: ActorSystem): ActorRef = system.actorOf(Props(new LocalCountManager))

}