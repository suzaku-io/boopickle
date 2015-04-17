package boopickle.perftests

import java.nio.ByteBuffer

import boopickle._

abstract class BooRunner[A](data: A) extends TestRunner[A](data) {
  override def name = "BooPickle"
}

object BooPickleRunners {

  def encodeRunner[A](data:A)(implicit p:Pickler[A], u:Unpickler[A]):BooRunner[A] = new BooRunner[A](data) {
    var testData : A = _

    override def initialize = {
      testData = data
      val bb = Pickle.intoBytes(testData)
      val ba = new Array[Byte](bb.limit)
      bb.get(ba)
      ba
    }

    override def run = {
      Pickle.intoBytes(testData)
    }
  }

  def decodeRunner[A](data:A)(implicit p:Pickler[A], u:Unpickler[A]):BooRunner[A] = new BooRunner[A](data) {
    var testData : A = _
    var bb:ByteBuffer = _

    override def initialize = {
      testData = data
      bb = Pickle.intoBytes(testData)
      val ba = new Array[Byte](bb.limit)
      bb.get(ba)
      bb.rewind()
      ba
    }

    override def run = {
      Unpickle[A].fromBytes(bb)
      bb.rewind()
    }
  }
}
