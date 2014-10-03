unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / "akka-actor-js" / "src" / "main" / "scala" / "akka" / "scalajs" / "wscommon"

resolvers += Resolver.url("scala-js-releases",
    url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
    Resolver.ivyStylePatterns)

libraryDependencies += "org.scalajs" %% "scalajs-pickling-play-json" % "0.3.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.6"
