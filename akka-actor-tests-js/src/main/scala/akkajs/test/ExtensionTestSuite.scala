package akkajs.test

import akka.actor.{ActorSystem, Extension, ExtensionKey, ExtendedActorSystem}

import scala.scalajs.js.annotation.JSExport


object MyExt extends ExtensionKey[Ext]

@JSExport
class Ext(system: ExtendedActorSystem) extends Extension {
  def itsMe = "Mario!"
}

class ExtensionTestSuite(system: ActorSystem) extends TestSuite with AsyncAssert {

  def numTests: Int = 1

  def testMain(): Unit = {
    val theExt = MyExt.createExtension(system.asInstanceOf[ExtendedActorSystem])
    assert(theExt.itsMe == "Mario!", s"extension invalid")
  }

}
