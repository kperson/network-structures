package udata

import com.typesafe.config.{ConfigFactory, Config}

/**
 * Created by keltonperson on 12/15/15.
 */
class LocalConfig {

  val config: Config = ConfigFactory.load().getConfig("udata-hub.local")

  def hasOrElse[A](key: String)(default: A)(has:(String) => A) : A = {
    if (config.hasPath(key)) {
      has(key)
    } else {
      default
    }
  }

  def rootDirectory = {
    hasOrElse("fs-directory")("/structure-data") { key =>
      config.getString(key)
    }
  }

}
