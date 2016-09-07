# This project is not maintained, a different one is though!
This project is not actively being worked on at present. 
Instead **an actively maintained port of Akka to Scala.js is available here: https://github.com/unicredit/akka.js**.

We highly recommend you have a look at it instead - it features a full Akka core as well as Streams port working in the browser.

# Akka.js

This project provides an implementation of Akka's core `akka-actor` module for
Scala.js. The project structure mirrors that of upstream Akka. The `akka-actor-js`
project contains the core actor implementation. An important goal is
to minimize source differences between upstream `akka-actor` and `akka-actor-js`.
The `akka-actor-tests-js` project provides the test suite for `akka-actor-js`.

## Status and plan

[Akka.js Status](https://docs.google.com/a/typesafe.com/document/d/1i2GbI3-_p-8nQdpyb9PbzeQzOKieDzPw9owkAJJ7cq4/edit#)

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
