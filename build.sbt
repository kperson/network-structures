lazy val commonSettings = Seq(
  organization := "com.kelt",
  version := "1",
  scalaVersion := "2.11.7",
  parallelExecution in Test := false
) ++ Revolver.settings


lazy val structures = (project in file("structures")).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "io.spray" %% "spray-can" % "1.3.3",
    "commons-io" % "commons-io" % "2.4",
    "com.typesafe.akka" %% "akka-actor" % "2.3.13",
    "io.spray" %% "spray-client" % "1.3.3",
    "io.spray" %% "spray-routing" % "1.3.3",
    "org.scalatra" %% "scalatra" % "2.3.1"
  ))


lazy val simpleDemo = (project in file("demo-simple")).
  settings(commonSettings: _*).
  dependsOn(structures)
