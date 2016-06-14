package boopickle.perftests

import java.nio.ByteBuffer

import boopickle.Default._
import boopickle.{DecoderSpeed, EncoderSpeed}

abstract class BooSpeedRunner[A](data: A) extends TestRunner[A](data) {
  override def name = "BooPickle!"
}

object BooPickleSpeedRunners {

  implicit def pickleState = new PickleState(new EncoderSpeed)
  implicit def unpickleState = (b: ByteBuffer) => new UnpickleState(new DecoderSpeed(b))

  def encodeRunner[A](data: A)(implicit p: Pickler[A]): BooSpeedRunner[A] = new BooSpeedRunner[A](data) {
    var testData: A = _

    override def initialize = {
      testData = data
      val bb = Pickle.intoBytes(testData)
      val ba = new Array[Byte](bb.limit)
      bb.get(ba)
      ba
    }

    override def run(): Unit = {
      Pickle.intoBytes(testData)
      ()
    }
  }

  def decodeRunner[A](data: A)(implicit p: Pickler[A]): BooSpeedRunner[A] = new BooSpeedRunner[A](data) {
    var testData: A = _
    var bb: ByteBuffer = _

    override def initialize = {
      testData = data
      bb = Pickle.intoBytes(testData)
      val ba = new Array[Byte](bb.limit)
      bb.get(ba)
      bb.rewind()
      ba
    }

    override def run: Unit = {
      Unpickle[A].fromBytes(bb)
      bb.rewind()
      ()
    }
  }
}
