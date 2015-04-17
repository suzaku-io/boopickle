package boopickle.perftests

import java.nio.charset.StandardCharsets

import prickle._

abstract class PrickleRunner[A](data: A) extends TestRunner[A](data) {
  override def name = "Prickle"
}

object PrickleRunners {

  def encodeRunner[A](data:A)(implicit p:Pickler[A], u:Unpickler[A]):PrickleRunner[A] = new PrickleRunner[A](data) {
    var testData : A = _

    override def initialize = {
      testData = data
      val r = Pickle.intoString(testData)
      // println(r)
      r.getBytes(StandardCharsets.UTF_8)
    }

    override def run = {
      Pickle.intoString(testData)
    }
  }

  def decodeRunner[A](data:A)(implicit p:Pickler[A], u:Unpickler[A]):PrickleRunner[A] = new PrickleRunner[A](data) {
    var testData : A = _
    var s:String = _

    override def initialize = {
      testData = data
      s = Pickle.intoString(testData)
      s.getBytes(StandardCharsets.UTF_8)
    }

    override def run = {
      Unpickle[A].fromString(s)
    }
  }
}
