# Akka.js

This project provides an implementation of Akka's core `akka-actor` module for
Scala.js. The project structure mirrors that of upstream Akka. The `akka-actor-js`
project contains the core actor implementation. An important goal is
to minimize source differences between upstream `akka-actor` and `akka-actor-js`.
The `akka-actor-tests-js` project provides the test suite for `akka-actor-js`.

## Building and running the test suite

The projects are built using `sbt`, using

```
> compile
```

The test suite is run using

```
> project akka-actor-tests-js
> run
```

## Building the example Play app

### Build the Scala.js client app (uses `akka-actor-js`)

```
> project playAppScalaJS
[info] Set current project to Scala.js play-app client ...
> fastOptJS
...
[info] Fast optimizing .../examples/play-app/public/javascripts/play-app-client-fastopt.js
[success] Total time: ...
```

### Build the Play server-side app (uses `akka-websocket-bridge`)

Quit the sbt console (if it’s still running). Change directory to the root of the Play app
(`examples/play-app`), and start a new sbt prompt. Build the Play app using:

```
[play-app] $ compile
```

Run the Play app using:

```
[play-app] $ run
```

The app is now running at `http://localhost:9000/`.
