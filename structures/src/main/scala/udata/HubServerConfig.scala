package udata

import com.typesafe.config.{ConfigFactory, Config}


class HubServerConfig(config: Config = ConfigFactory.load().getConfig("udata-hub")) {

  def countManagerClassName = config.getString("count-manager")

}