package kelt.structures.util

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Millis, Span}

import scala.concurrent.duration.FiniteDuration


object TimeoutConversions {

  implicit def toTestTimeout(duration: FiniteDuration) =  Timeout(Span(duration.toMillis, Millis))

}
