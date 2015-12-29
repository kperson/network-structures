package udata

import com.typesafe.config.{ConfigFactory, Config}


class HubServerConfig() {

  val config: Config = ConfigFactory.load().getConfig("udata-hub")

  def countManagerClassName = config.getString("count-manager")
  def lockManagerClassName = config.getString("lock-manager")
  def directoryManagerClassName = config.getString("dir-manager")
  def pubSubManagerClassName = config.getString("pubsub-manager")

  def actorSystemName = config.getString("actor-system")

  def hasOrElse[A](key: String)(default: A)(has:(String) => A) : A = {
    if (config.hasPath(key)) {
      has(key)
    } else {
      default
    }
  }

}

object HubServerConfig {

  lazy val serverConfig = new HubServerConfig()

}