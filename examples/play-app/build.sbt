name := "play-app"

version := "1.0-SNAPSHOT"

lazy val root = Project("play-app", file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",
  "org.scalajs" %% "scalajs-pickling-play-json" % "0.3.1"
)

unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / ".." / "akka-websocket-bridge" / "src" / "main" / "scala" / "akkajs" / "wsserver"

unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / ".." / "akka-websocket-scalajs" / "src" / "main" / "scala" / "akka" / "scalajs" / "wscommon"

unmanagedSourceDirectories in Compile += baseDirectory.value / "cscommon" / "models"
