package udata.util

import akka.actor.ActorSystem
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Millis, Span}

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions


object TestUtils {

  implicit def toTestTimeout(duration: FiniteDuration) =  Timeout(Span(duration.toMillis, Millis))


  def withActorSystem(testCode: (ActorSystem) => Any): Unit = {

    val sys = ActorSystem(java.util.UUID.randomUUID.toString.replace("-", "").substring(0, 5))
    testCode(sys)
    sys.shutdown()
  }

}