import akkajs.AkkaJSBuild

//import akkajs.Exclude

import scala.scalajs.sbtplugin.ScalaJSPlugin._ // import `%%%` extension method

scalaJSSettings

AkkaJSBuild.defaultSettings

unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / "akka-actor" / "src" / "main" / "scala"

excludeFilter in unmanagedSources := HiddenFileFilter //|| Exclude.excludeFromModuleOne

ScalaJSKeys.persistLauncher := true

ScalaJSKeys.persistLauncher in Test := false

ScalaJSKeys.preLinkJSEnv := new scala.scalajs.sbtplugin.env.phantomjs.PhantomJSEnv(autoExit = false)

ScalaJSKeys.postLinkJSEnv := new scala.scalajs.sbtplugin.env.phantomjs.PhantomJSEnv(autoExit = false)

libraryDependencies += "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6" // MIT

libraryDependencies += "org.scalajs" %%% "scalajs-pickling" % "0.3.1"
