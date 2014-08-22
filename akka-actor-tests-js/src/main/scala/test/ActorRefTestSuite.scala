package akkajs.test

import akka.actor.Actor


class ActorRefTestSuite extends TestSuite with AsyncAssert {

  def numTests: Int = 1

  def testMain(): Unit = {
    intercept[akka.actor.ActorInitializationException] {
      new Actor { def receive = { case _ ⇒ } }
    }
  }

}
