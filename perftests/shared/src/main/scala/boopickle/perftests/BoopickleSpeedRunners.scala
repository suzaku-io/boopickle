package boopickle.perftests

import java.nio.ByteBuffer

import boopickle.Default._
import boopickle.{BufferPool, DecoderSpeed, EncoderSpeed}
import org.openjdk.jmh.annotations.Benchmark

abstract class BooSpeedRunner[A](data: A) extends TestRunner[A](data) {
  override def name = "BooPickle!"
}

object BooPickleSpeedRunners {

  implicit def pickleState   = new PickleState(new EncoderSpeed, false, false)
  implicit def unpickleState = (b: ByteBuffer) => new UnpickleState(new DecoderSpeed(b), false, false)

  def encodeRunner[A](data: A)(implicit p: Pickler[A]): BooSpeedRunner[A] = new BooSpeedRunner[A](data) {
    var testData: A = _

    override def initialize = {
      BufferPool.enable()
      testData = data
      val bb = Pickle.intoBytes(testData)
      val ba = new Array[Byte](bb.limit)
      bb.get(ba)
      ba
    }

    override def run(): Any = {
      val bb = Pickle.intoBytes(testData)
      BufferPool.release(bb)
      bb
    }
  }

  def decodeRunner[A](data: A)(implicit p: Pickler[A]): BooSpeedRunner[A] = new BooSpeedRunner[A](data) {
    var testData: A    = _
    var bb: ByteBuffer = _

    override def initialize = {
      BufferPool.enable()
      testData = data
      bb = Pickle.intoBytes(testData)
      val ba = new Array[Byte](bb.limit)
      bb.get(ba)
      bb.rewind()
      ba
    }

    override def run: Any = {
      val a = Unpickle[A].fromBytes(bb)
      bb.rewind()
      a
    }
  }
}

trait BoopickleSpeedCoding { self: TestData =>
  private implicit def pickleState   = new PickleState(new EncoderSpeed, false, false)
  private implicit val unpickleState = (b: ByteBuffer) => new UnpickleState(new DecoderSpeed(b), false, false)

  lazy val eventBB: ByteBuffer   = Pickle.intoBytes(event)
  lazy val intsBB: ByteBuffer    = Pickle.intoBytes(largeIntSeq)
  lazy val doublesBB: ByteBuffer = Pickle.intoBytes(largeDoubleSeq)

  @Benchmark
  def boopickleSpeedEventDecode: Event = {

    val event = Unpickle[Event].fromBytes(eventBB)
    eventBB.rewind()
    event
  }

  @Benchmark
  def boopickleSpeedEventEncode: ByteBuffer = {
    val bb = Pickle.intoBytes(event)
    BufferPool.release(bb)
    bb
  }

  @Benchmark
  def boopickleSpeedIntArrayDecode: Array[Int] = {
    val a = Unpickle[Array[Int]].fromBytes(intsBB)
    intsBB.rewind()
    a
  }

  @Benchmark
  def boopickleSpeedIntArrayEncode: ByteBuffer = {
    val bb = Pickle.intoBytes(largeIntSeq)
    BufferPool.release(bb)
    bb
  }

  @Benchmark
  def boopickleSpeedDoubleArrayDecode: Array[Double] = {
    val a = Unpickle[Array[Double]].fromBytes(doublesBB)
    doublesBB.rewind()
    a
  }

  @Benchmark
  def boopickleSpeedDoubleArrayEncode: ByteBuffer = {
    val bb = Pickle.intoBytes(largeDoubleSeq)
    BufferPool.release(bb)
    bb
  }
}
