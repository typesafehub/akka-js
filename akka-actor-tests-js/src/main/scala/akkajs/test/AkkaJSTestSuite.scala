package akkajs.test

import scala.scalajs.js
import scala.scalajs.js.Dynamic.global

import akka.actor.ActorSystem


object AkkaJSTestSuite extends js.JSApp {

  def main(): Unit = {
    var testsTotal: Int = 0

    global.console.log("Creating actor system...")
    val system = ActorSystem("test-system")

    val actorTestSuite = new ActorTestSuite(system)
    testsTotal += actorTestSuite.numTests

    val actorRefTestSuite = new ActorRefTestSuite(system)
    testsTotal += actorRefTestSuite.numTests

    val dynAccessTestSuite = new DynamicAccessTestSuite
    testsTotal += dynAccessTestSuite.numTests

    val extTestSuite = new ExtensionTestSuite(system)
    testsTotal += extTestSuite.numTests

    TestSuite.after(testsTotal)(DefaultConsolePrinter andThen System.exit)

    actorTestSuite.testMain()
    actorRefTestSuite.testMain()
    dynAccessTestSuite.testMain()
    extTestSuite.testMain()
  }

}
