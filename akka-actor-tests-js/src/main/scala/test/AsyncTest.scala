package akkajs.test

import scala.reflect.{ClassTag, classTag}
import scala.util.{Try, Success, Failure}


object TestSuite {

  private var tasks: List[TestTask] = List()

  private var resultHandler: List[CompletedTest] => Unit =
    (tests: List[CompletedTest]) => { /* do nothing */ }

  private var numTests: Int = _

  def checkLater(task: TestTask): Unit = {
    tasks = task :: tasks
    if (tasks.length == numTests) {
      val results = tasks.map { task =>
        val result = Try {
          val success = task.cond()
          if (!success) throw new AssertionError(task.msg())
          success
        }
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

  def intercept[T <: Throwable: ClassTag](body: => Unit): Unit = {
    val outcome: Try[Boolean] = try {
      body
      Failure(new IllegalStateException("no exception thrown"))
    } catch {
      case t: Throwable =>
        val clazz = implicitly[ClassTag[T]].runtimeClass
        val catchClass = t.getClass
        // t.getClass <: classOf[T]?
        if (clazz.isAssignableFrom(catchClass))
          Success(true)
        else
          Failure(t)
    }
    assert(outcome.isSuccess, s"unexpected ${outcome.failed.get.toString}")
  }

  def checkIntercept[E <: Throwable: ClassTag, T](outcome: Try[T]): Unit = outcome match {
    case Success(_) =>
      assert(false, s"${classTag[E].runtimeClass.toString} not thrown}")
    case Failure(t) =>
      val toReport: Try[Boolean] = {
        val clazz = classTag[E].runtimeClass
        val catchClass = t.getClass
        // t.getClass <: classOf[E]?
        if (clazz.isAssignableFrom(catchClass))
          Success(true)
        else
          Failure(t)
      }
      assert(toReport.isSuccess, s"unexpected ${toReport.failed.get.toString}")
  }

}

trait TestSuite {
  def testMain(): Unit
  def numTests: Int
}

object DefaultConsolePrinter extends (List[CompletedTest] => Int) {

  def apply(tests: List[CompletedTest]): Int = {
    var exitCode = 0
    tests.zipWithIndex.foreach { case (test, index) =>
      if (test.result.isFailure)
        exitCode = 1
      println(s"Test $index: ${test.result}")
    }
    exitCode
  }

}
