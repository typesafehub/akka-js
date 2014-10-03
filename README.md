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
