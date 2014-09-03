import akkajs.{AkkaJSBuild, Formatting}

//import akkajs.Exclude

import scala.scalajs.sbtplugin.ScalaJSPlugin._ // import `%%%` extension method

scalaJSSettings

AkkaJSBuild.defaultSettings

Formatting.formatSettings

unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / "akka-actor" / "src" / "main" / "scala"

excludeFilter in unmanagedSources := HiddenFileFilter //|| Exclude.excludeFromModuleOne

ScalaJSKeys.persistLauncher := false

// only for testing:

libraryDependencies += "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6" // MIT

libraryDependencies += "org.scalajs" %%% "scalajs-pickling" % "0.3.1"
