/**
 * Copyright (C) 2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.actor

import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.dom

object ScalaJSExample extends js.JSApp {
  def main(): Unit = {
    val paragraph = dom.document.createElement("p")
    val result = "hello there, kiddo"
    paragraph.innerHTML = s"<strong>$result</strong>"
    dom.document.getElementById("playground").appendChild(paragraph)
  }

  def m(): Unit = {
    error("boom!") // `Predef.error` is deprecated
  }
}
