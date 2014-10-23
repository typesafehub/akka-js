import scala.scalajs.sbtplugin.ScalaJSPlugin._ // import `%%%` extension method

scalaJSSettings

ScalaJSKeys.persistLauncher := false

libraryDependencies += "org.scalajs" %%% "scalajs-pickling" % "0.3.1"
