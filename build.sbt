import spray.revolver.RevolverPlugin.Revolver

resolvers += Resolver.sonatypeRepo("public")

lazy val commonSettings = Seq(
  organization := "com.kelt.structures",
  version := "1",
  scalaVersion := "2.11.7",
  parallelExecution in Test := false,
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
)


lazy val structures = (project in file("structures")).
  settings(commonSettings).
  settings(libraryDependencies ++= Seq(
    "org.scalatest"     %% "scalatest"      % "2.2.4"   % "test",
    "io.spray"          %% "spray-can"      % "1.3.3",
    "commons-io"        % "commons-io"      % "2.4",
    "com.typesafe.akka" %% "akka-actor"     % "2.3.13",
    "io.spray"          %% "spray-client"   % "1.3.3",
    "io.spray"          %% "spray-routing"  % "1.3.3",
    "org.scalatra"      %% "scalatra"       % "2.3.1"
  ))


lazy val hub = (project in file("hub")).
  enablePlugins(JavaAppPackaging).
  settings(commonSettings).
  settings(libraryDependencies ++= Seq(
    "com.github.scopt"  %%  "scopt"         % "3.3.0"
  )).
  settings(Revolver.settings).
  dependsOn(structures)