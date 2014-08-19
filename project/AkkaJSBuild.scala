/**
 * Copyright (C) 2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akkajs

import sbt._
import Keys._

object AkkaJSBuild extends Build {

  lazy val buildSettings = Seq(
    organization := "org.scala-lang.modules.akkajs",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := "2.11.2" // TODO: move to Dependencies.Versions.scalaVersion
  )

  lazy val akkaActorJS = Project(
    id = "akka-actor-js",
    base = file("akka-actor-js")
  )

  lazy val akkaActorTestsJS = Project(
    id = "akka-actor-tests-js",
    base = file("akka-actor-tests-js")
  ) dependsOn(akkaActorJS)

  override lazy val settings =
    super.settings ++
    buildSettings

  lazy val defaultSettings = Seq(
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint")
  )

}
