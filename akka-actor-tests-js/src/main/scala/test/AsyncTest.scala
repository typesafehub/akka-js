package akkajs.test

import scala.util.Try


object TestSuite {

  private var tasks: List[TestTask] = List()

  private var resultHandler: List[CompletedTest] => Unit =
    (tests: List[CompletedTest]) => { /* do nothing */ }

  private var numTests: Int = _

  def checkLater(task: TestTask): Unit = {
    tasks = task :: tasks
    if (tasks.length == numTests) {
      val results = tasks.map { task =>
        val result = Try { task.cond() }
        CompletedTest(task, result)
      }

      resultHandler(results)
    }
  }

  def after(num: Int)(handler: List[CompletedTest] => Unit): Unit = {
    resultHandler = handler
    numTests = num
  }

}

case class TestTask(cond: () => Boolean, msg: () => String)
case class CompletedTest(task: TestTask, result: Try[Boolean])

trait AsyncAssert {

  def assert(cond: => Boolean, msg: => String): Unit = {
    TestSuite.checkLater(TestTask(() => cond, () => msg))
  }

}

object DefaultConsolePrinter extends (List[CompletedTest] => Unit) {

  def apply(tests: List[CompletedTest]): Unit = {
    tests.zipWithIndex.foreach { case (test, index) =>
      val info = if (test.result.isFailure)
        s": ${test.task.msg()}"
      else
        ""
      println(s"Test $index: ${test.result}$info")
    }
  }

}
