package akkajs.test

import scala.scalajs.js

object AkkaJSTestSuite extends js.JSApp {

  def main(): Unit = {
    var testsTotal: Int = 0

    val actorTestSuite = new ActorTestSuite
    testsTotal += actorTestSuite.numTests

    val actorRefTestSuite = new ActorRefTestSuite
    testsTotal += actorRefTestSuite.numTests

    TestSuite.after(testsTotal)(DefaultConsolePrinter)

    actorTestSuite.testMain()
    actorRefTestSuite.testMain()
  }

}
