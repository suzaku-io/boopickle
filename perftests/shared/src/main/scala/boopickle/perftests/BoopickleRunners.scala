package boopickle.perftests

import java.nio.ByteBuffer

import boopickle.{BufferPool, DecoderSize, EncoderSize}
import boopickle.Default._
import org.openjdk.jmh.annotations._

abstract class BooRunner[A](data: A) extends TestRunner[A](data) {
  override def name = "BooPickle"
}

object BooPickleRunners {

  def encodeRunner[A](data: A)(implicit p: Pickler[A]): BooRunner[A] = new BooRunner[A](data) {
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

  def decodeRunner[A](data: A)(implicit p: Pickler[A]): BooRunner[A] = new BooRunner[A](data) {
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

    override def run(): Any = {
      val a = Unpickle[A].fromBytes(bb)
      bb.rewind()
      a
    }
  }
}

trait BoopickleCoding { self: TestData =>
  private implicit def pickleState   = new PickleState(new EncoderSize, false, false)
  private implicit val unpickleState = (b: ByteBuffer) => new UnpickleState(new DecoderSize(b), false, false)

  lazy val eventBB: ByteBuffer   = Pickle.intoBytes(event)
  lazy val intsBB: ByteBuffer    = Pickle.intoBytes(largeIntSeq)
  lazy val doublesBB: ByteBuffer = Pickle.intoBytes(largeDoubleSeq)

  @Benchmark
  def boopickleEventDecode: Event = {
    val event = Unpickle[Event].fromBytes(eventBB)
    eventBB.rewind()
    event
  }

  @Benchmark
  def boopickleEventEncode: ByteBuffer = {
    val bb = Pickle.intoBytes(event)
    BufferPool.release(bb)
    bb
  }

  @Benchmark
  def boopickleIntArrayDecode: Array[Int] = {
    val a = Unpickle[Array[Int]].fromBytes(intsBB)
    intsBB.rewind()
    a
  }

  @Benchmark
  def boopickleIntArrayEncode: ByteBuffer = {
    val bb = Pickle.intoBytes(largeIntSeq)
    BufferPool.release(bb)
    bb
  }

  @Benchmark
  def boopickleDoubleArrayDecode: Array[Double] = {
    val a = Unpickle[Array[Double]].fromBytes(doublesBB)
    doublesBB.rewind()
    a
  }

  @Benchmark
  def boopickleDoubleArrayEncode: ByteBuffer = {
    val bb = Pickle.intoBytes(largeDoubleSeq)
    BufferPool.release(bb)
    bb
  }
}
