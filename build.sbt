import spray.revolver.RevolverPlugin.Revolver

resolvers += Resolver.sonatypeRepo("public")

@inline def env(n: String): Option[String] = sys.env.get(n)

lazy val commonSettings = Seq(
  organization := "com.udata",
  version := "0.0.11-SNAPSHOT",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  publishTo := {
    if (isSnapshot.value)
      Some(Resolver.file("file", new File(env("FILE_PUBLISH").getOrElse(Path.userHome.getAbsolutePath + "/Dropbox/Public/maven/") + "snapshot")))
    else
      Some(Resolver.file("file", new File(env("FILE_PUBLISH").getOrElse(Path.userHome.getAbsolutePath + "/Dropbox/Public/maven/") + "release")))
  }
)


lazy val structures = (project in file("structures")).
  settings(
    fork in Test := true,
    parallelExecution in Test := false
  ).
  settings(commonSettings).
  settings(
    libraryDependencies ++= Seq(
      "org.scalatest"     %% "scalatest"      % "2.2.4",
      "io.spray"          %% "spray-can"      % "1.3.3",
      "commons-io"        %  "commons-io"     % "2.4",
      "com.typesafe.akka" %% "akka-actor"     % "2.3.13",
      "io.spray"          %% "spray-client"   % "1.3.3",
      "io.spray"          %% "spray-routing"  % "1.3.3",
      "org.scalatra"      %% "scalatra"       % "2.3.1",
      "com.gilt"          %% "jerkson"        % "0.6.8"
    )
  )


lazy val hub = (project in file("hub")).
  enablePlugins(JavaAppPackaging).
  settings(commonSettings).
  settings(libraryDependencies ++= Seq(
    "com.github.scopt"  %%  "scopt"         % "3.3.0"
  )).
  settings(Revolver.settings).
  dependsOn(structures)
