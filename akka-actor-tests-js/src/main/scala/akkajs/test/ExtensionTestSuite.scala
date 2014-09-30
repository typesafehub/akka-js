package akkajs.test

import akka.actor.{ActorSystem, Extension, ExtensionKey, ExtendedActorSystem}

import scala.scalajs.js.annotation.JSExport


object MyExt extends ExtensionKey[Ext]

@JSExport
class Ext(system: ExtendedActorSystem) extends Extension {
  def itsMe = "Mario!"
}

class ExtensionTestSuite(system: ActorSystem) extends TestSuite with AsyncAssert {

  def numTests: Int = 4

  def testMain(): Unit = {
    test("ExtensionId#createExtension") { implicit desc: TestDesc =>
      val theExt = MyExt.createExtension(system.asInstanceOf[ExtendedActorSystem])
      assert1(theExt.itsMe == "Mario!", s"extension invalid")
    }

    val extension = test("ExtensionId#apply") { implicit desc: TestDesc =>
      val ext = MyExt(system)
      assert1(ext.itsMe == "Mario!", s"extension not registered correctly")
      ext
    }

    test("ActorSystem#hasExtension") { implicit desc: TestDesc =>
      assert1(system.hasExtension(MyExt) == true, s"system must have registered extension after invoking apply")
    }

    test("ActorSystem#extension") { implicit desc: TestDesc =>
      assert1(system.extension(MyExt) == extension, s"could not look up extension correctly")
    }
  }

}
