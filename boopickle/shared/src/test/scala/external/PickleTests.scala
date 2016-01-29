package external

import java.nio.{ByteOrder, ByteBuffer}
import java.util.UUID

import boopickle.Default._
import boopickle._
import utest._

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.util.Random

object PickleTests extends TestSuite {
  override def tests = TestSuite {
    'Boolean - {
      'true {
        val bb = Pickle.intoBytes(true)
        assert(bb.limit == 1)
        assert(Unpickle[Boolean].fromBytes(bb))
      }
      'false {
        val bb = Pickle.intoBytes(false)
        assert(bb.limit == 1)
        assert(!Unpickle[Boolean].fromBytes(bb))
      }
    }
    'Byte - {
      'zero {
        val bb = Pickle.intoBytes(0.toByte)
        assert(bb.limit == 1)
        assert(Unpickle[Byte].fromBytes(bb) == 0)
      }
      'positive {
        val bb = Pickle.intoBytes(50.toByte)
        assert(bb.limit == 1)
        assert(Unpickle[Byte].fromBytes(bb) == 50)
      }
      'negative {
        val bb = Pickle.intoBytes(-1.toByte)
        assert(bb.limit == 1)
        assert(Unpickle[Byte].fromBytes(bb) == -1)
      }
    }
    'Short - {
      'zero {
        val bb = Pickle.intoBytes(0.toShort)
        assert(bb.limit == 1)
        assert(Unpickle[Short].fromBytes(bb) == 0)
      }
      'positive {
        val bb = Pickle.intoBytes(0x4000.toShort)
        assert(bb.limit == 3)
        assert(Unpickle[Short].fromBytes(bb) == 0x4000)
      }
      'negative {
        val bb = Pickle.intoBytes(-1.toShort)
        assert(bb.limit == 2)
        assert(Unpickle[Short].fromBytes(bb) == -1)
      }
    }
    'Int - {
      'zero {
        val bb = Pickle.intoBytes(0)
        assert(bb.limit == 1)
        assert(Unpickle[Int].fromBytes(bb) == 0)
      }
      'positive {
        val bb = Pickle.intoBytes(1024)
        assert(bb.limit == 2)
        assert(Unpickle[Int].fromBytes(bb) == 1024)
      }
      'negative {
        val bb = Pickle.intoBytes(-1048577)
        assert(bb.limit == 4)
        assert(Unpickle[Int].fromBytes(bb) == -1048577)
      }
      'max {
        val bb = Pickle.intoBytes(Int.MaxValue)
        assert(bb.limit == 5)
        assert(Unpickle[Int].fromBytes(bb) == Int.MaxValue)
      }
      'min {
        val bb = Pickle.intoBytes(Int.MinValue)
        assert(bb.limit == 5)
        assert(Unpickle[Int].fromBytes(bb) == Int.MinValue)
      }
    }
    'Long - {
      'positive {
        val bb = Pickle.intoBytes(1024L * 1024L * 1024L * 1024L)
        assert(bb.limit == 9)
        assert(Unpickle[Long].fromBytes(bb) == 1024L * 1024L * 1024L * 1024L)
      }
      'negative {
        val bb = Pickle.intoBytes(-1024L * 1024L * 1024L * 1024L)
        assert(bb.limit == 9)
        assert(Unpickle[Long].fromBytes(bb) == -1024L * 1024L * 1024L * 1024L)
      }
      'max {
        val bb = Pickle.intoBytes(Long.MaxValue)
        assert(bb.limit == 9)
        assert(Unpickle[Long].fromBytes(bb) == Long.MaxValue)
      }
      'min {
        val bb = Pickle.intoBytes(Long.MinValue)
        assert(bb.limit == 9)
        assert(Unpickle[Long].fromBytes(bb) == Long.MinValue)
      }
    }
    'Float - {
      'positive {
        val bb = Pickle.intoBytes(0.185f)
        assert(bb.limit == 4)
        assert(Unpickle[Float].fromBytes(bb) == 0.185f)
      }
      'negative {
        val bb = Pickle.intoBytes(-0.185f)
        assert(bb.limit == 4)
        assert(Unpickle[Float].fromBytes(bb) == -0.185f)
      }
      'max {
        val bb = Pickle.intoBytes(Float.MaxValue)
        assert(bb.limit == 4)
        assert(Unpickle[Float].fromBytes(bb) == Float.MaxValue)
      }
      'min {
        val bb = Pickle.intoBytes(Float.MinValue)
        assert(bb.limit == 4)
        assert(Unpickle[Float].fromBytes(bb) == Float.MinValue)
      }
      'infinity {
        val bb = Pickle.intoBytes(Float.PositiveInfinity)
        assert(bb.limit == 4)
        assert(Unpickle[Float].fromBytes(bb) == Float.PositiveInfinity)
      }
    }
    'Double - {
      'positive {
        val bb = Pickle.intoBytes(math.Pi)
        assert(bb.limit == 8)
        assert(Unpickle[Double].fromBytes(bb) == math.Pi)
      }
      'negative {
        val bb = Pickle.intoBytes(-math.Pi)
        assert(bb.limit == 8)
        assert(Unpickle[Double].fromBytes(bb) == -math.Pi)
      }
      'max {
        val bb = Pickle.intoBytes(Double.MaxValue)
        assert(bb.limit == 8)
        assert(Unpickle[Double].fromBytes(bb) == Double.MaxValue)
      }
      'min {
        val bb = Pickle.intoBytes(Double.MinValue)
        assert(bb.limit == 8)
        assert(Unpickle[Double].fromBytes(bb) == Double.MinValue)
      }
      'infinity {
        val bb = Pickle.intoBytes(Double.PositiveInfinity)
        assert(bb.limit == 8)
        assert(Unpickle[Double].fromBytes(bb) == Double.PositiveInfinity)
      }
    }
    'BigInt - {
      'zero {
        val bb = Pickle.intoBytes(BigInt(0))
        assert(bb.limit == 2)
        val bi = Unpickle[BigInt].fromBytes(bb)
        assert(bi == BigInt(0))
      }
      'positive {
        val value = BigInt("3031082301820398102312310273912739712397")
        val bb = Pickle.intoBytes(value)
        assert(bb.limit == value.toByteArray.size + 1)
        assert(Unpickle[BigInt].fromBytes(bb) == value)
      }
      'negative {
        val value = BigInt("-3031082301820398102312310273912739712397")
        val bb = Pickle.intoBytes(value)
        assert(bb.limit == value.toByteArray.size + 1)
        assert(Unpickle[BigInt].fromBytes(bb) == value)
      }
    }
    'BigDecimal - {
      'zero {
        val bb = Pickle.intoBytes(BigDecimal(0))
        assert(bb.limit == 3)
        assert(Unpickle[BigDecimal].fromBytes(bb) == BigDecimal(0))
      }
      'positive {
        val value = BigDecimal("3031082301820398102312310273912739712397.420348203423429374928374")
        val bb = Pickle.intoBytes(value)
        val expectedLimit = value.underlying.unscaledValue.toByteArray.size + 1 + 1
        assert(bb.limit == expectedLimit)
        assert(Unpickle[BigDecimal].fromBytes(bb) == value)
      }
      'positiveZeroScale {
        val value = BigDecimal("3031082301820398102312310273912739712397420348203423429374928374")
        val bb = Pickle.intoBytes(value)
        val expectedLimit = value.underlying.unscaledValue.toByteArray.size + 1 + 1
        assert(bb.limit == expectedLimit)
        assert(Unpickle[BigDecimal].fromBytes(bb) == value)
      }
      'negativeScale {
        val value = BigDecimal("-3031082301820398102312310273912739712397.420348203423429374928374")
        val bb = Pickle.intoBytes(value)
        val expectedLimit = value.underlying.unscaledValue.toByteArray.size + 1 + 1
        assert(bb.limit == expectedLimit)
        assert(Unpickle[BigDecimal].fromBytes(bb) == value)
      }
      'negativeZeroScale {
        val value = BigDecimal("-3031082301820398102312310273912739712397420348203423429374928374")
        val bb = Pickle.intoBytes(value)
        val expectedLimit = value.underlying.unscaledValue.toByteArray.size + 1 + 1
        assert(bb.limit == expectedLimit)
        assert(Unpickle[BigDecimal].fromBytes(bb) == value)
      }
    }
    'String - {
      'null {
        val str: String = null
        val bb = Pickle.intoBytes(str)
        assert(bb.limit == 2)
        assert(Unpickle[String].fromBytes(bb) == null)
      }
      'empty {
        val bb = Pickle.intoBytes("")
        assert(bb.limit == 1)
        assert(Unpickle[String].fromBytes(bb) == "")
      }
      'normal {
        val bb = Pickle.intoBytes("normal")
        assert(bb.limit == 1 + 6)
        assert(Unpickle[String].fromBytes(bb) == "normal")
      }
      'numeric {
        val bb = Pickle.intoBytes("100")
        assert(bb.limit == 1 + 1)
        assert(Unpickle[String].fromBytes(bb) == "100")
      }
      'numericSmall {
        val bb = Pickle.intoBytes("5")
        assert(bb.limit == 2)
        assert(Unpickle[String].fromBytes(bb) == "5")
      }
      'numericLarge {
        val bb = Pickle.intoBytes("-10000000000")
        assert(bb.limit == 1 + 8)
        assert(Unpickle[String].fromBytes(bb) == "-10000000000")
      }
      'numeric20 {
        val bb = Pickle.intoBytes("45248643522829592471")
        // assert(bb.limit == 1 + 8)
        assert(Unpickle[String].fromBytes(bb) == "45248643522829592471")
      }
      'numericStart {
        val bb = Pickle.intoBytes("100x")
        assert(bb.limit == 1 + 4)
        assert(Unpickle[String].fromBytes(bb) == "100x")
      }
      'numericZeros {
        val bb = Pickle.intoBytes("0100")
        assert(bb.limit == 1 + 4)
        assert(Unpickle[String].fromBytes(bb) == "0100")
      }
      'uuid {
        val uuidStr = UUID.randomUUID().toString
        val bb = Pickle.intoBytes(uuidStr)
        assert(bb.limit == 1 + 16)
        assert(Unpickle[String].fromBytes(bb) == uuidStr)
      }
      'uuidUpperCase {
        val uuidStr = UUID.randomUUID().toString.toUpperCase
        val bb = Pickle.intoBytes(uuidStr)
        assert(bb.limit == 1 + 16)
        assert(Unpickle[String].fromBytes(bb) == uuidStr)
      }
    }
    'Option - {
      'some {
        val bb = Pickle.intoBytes(Some("test"))
        assert(bb.limit == 1 + 5)
        assert(Unpickle[Option[String]].fromBytes(bb).contains("test"))
      }
      'none {
        val d: Option[String] = None
        val bb = Pickle.intoBytes(d)
        assert(bb.limit == 2)
        assert(Unpickle[Option[String]].fromBytes(bb).isEmpty)
      }
    }
    'Duration - {
      'finite {
        val bb = Pickle.intoBytes(Duration.fromNanos(1000))
        assert(bb.limit == 2)
        assert(Unpickle[Duration].fromBytes(bb) == Duration.fromNanos(1000))
      }
      'finiteLarge {
        val bb = Pickle.intoBytes(Duration.fromNanos(Int.MaxValue * 2L))
        assert(bb.limit == 9)
        assert(Unpickle[Duration].fromBytes(bb) == Duration.fromNanos(Int.MaxValue * 2L))
      }
      'infinite {
        val bb = Pickle.intoBytes(Duration.Inf)
        assert(bb.limit == 1)
        assert(Unpickle[Duration].fromBytes(bb) == Duration.Inf)
      }
      'minusInfinite {
        val bb = Pickle.intoBytes(Duration.MinusInf)
        assert(bb.limit == 1)
        assert(Unpickle[Duration].fromBytes(bb) == Duration.MinusInf)
      }
      'undefined {
        val bb = Pickle.intoBytes(Duration.Undefined)
        assert(bb.limit == 1)
        assert(Unpickle[Duration].fromBytes(bb) eq Duration.Undefined)
      }
    }
    'UUID - {
      'random {
        val uuid = UUID.randomUUID()
        val bb = Pickle.intoBytes(uuid)
        assert(bb.limit == 16)
        assert(Unpickle[UUID].fromBytes(bb) == uuid)
      }
    }
    'Either - {
      'left {
        val e:Either[Int, String] = Left(5)
        val bb = Pickle.intoBytes(e)
        assert(bb.limit == 1 + 1)
        assert(Unpickle[Either[Int, String]].fromBytes(bb) == Left(5))
      }
      'right {
        val e:Either[Int, String] = Right("Error!")
        val bb = Pickle.intoBytes(e)
        assert(bb.limit == 1 + 7)
        assert(Unpickle[Either[Int, String]].fromBytes(bb) == Right("Error!"))
      }
    }
    'Tuples - {
      'tuple2 {
        val bb = Pickle.intoBytes(("Hello", Some("World")))
        assert(bb.limit == 6 + 1 + 6)
        assert(Unpickle[(String, Option[String])].fromBytes(bb) == ("Hello", Some("World")))
      }
    }
    'Seq - {
      'empty {
        val bb = Pickle.intoBytes(Seq.empty[Int])
        assert(bb.limit == 1)
        assert(Unpickle[Seq[Int]].fromBytes(bb) == Seq.empty[Int])
      }
      'nulls {
        val bb = Pickle.intoBytes(Seq[String](null, "Test"))
        assert(bb.limit == 1 + 2 + 5)
        assert(Unpickle[Seq[String]].fromBytes(bb) == Seq[String](null, "Test"))
      }
      'singleEntry {
        val bb = Pickle.intoBytes(Seq(4095))
        assert(bb.limit == 3)
        assert(Unpickle[Seq[Int]].fromBytes(bb) == Seq(4095))
      }
      'seqOfSeq {
        val bb = Pickle.intoBytes(Seq(Seq(1), Seq(1, 2)))
        assert(bb.limit == 1 + 2 + 3) // seq length, first subseq, second subseq
        assert(Unpickle[Seq[Seq[Int]]].fromBytes(bb) == Seq(Seq(1), Seq(1, 2)))
      }
      'dupStrings {
        val bb = Pickle.intoBytes(Seq("test", "test", "test", "test"))
        assert(bb.limit == 1 + 5 + 3 * 2) // seq length, first "test", 3x reference
        assert(Unpickle[Seq[String]].fromBytes(bb) == Seq("test", "test", "test", "test"))
      }
      'longSeq {
        val data = Vector.tabulate[Int](10000)(x => -x)
        val bb = Pickle.intoBytes(data)
        assert(bb.limit == 25906)
        val u = Unpickle[Vector[Int]].fromBytes(bb)
        assert(u == data)
      }
      'dupEmpty {
        val data = Seq(Seq(), Seq("test"), Seq("test"), Seq(), Seq())
        val bb = Pickle.intoBytes(data)
        assert(Unpickle[Seq[Seq[String]]].fromBytes(bb) == data)
      }
      'tuples {
        val data: List[(String, String)] = List(("A", "B"), ("B", "C"))
        val bb = Pickle.intoBytes(data)
        val u = Unpickle[List[(String, String)]].fromBytes(bb)
        assert(u == data)
      }
    }
    'Map - {
      'empty {
        val bb = Pickle.intoBytes(Map.empty[String, Int])
        assert(bb.limit == 1)
        assert(Unpickle[Map[String, Int]].fromBytes(bb) == Map.empty[String, Int])
      }
      'simple {
        val bb = Pickle.intoBytes(Map(1 -> 2, 5 -> 8000))
        assert(bb.limit == 1 + 2 + 4)
        assert(Unpickle[Map[Int, Int]].fromBytes(bb) == Map(1 -> 2, 5 -> 8000))
      }
      'mutable {
        val bb = Pickle.intoBytes(mutable.HashMap(1 -> 2))
        assert(bb.limit == 3)
        assert(Unpickle[mutable.HashMap[Int, Int]].fromBytes(bb) == mutable.HashMap(1 -> 2))
      }
      'large {
        val largeStringIntMap:Map[String, Int] = {
          val r = new Random(0)
          (for(i <- 0 until 10000) yield s"ID$i" -> (1.0/(1.0 + r.nextDouble()*1e5)*1e7).toInt).toMap
        }
        val bb = Pickle.intoBytes(largeStringIntMap)
        assert(Unpickle[Map[String, Int]].fromBytes(bb) == largeStringIntMap)
      }
      'complex {
        val testMap = Map[String, Map[String, Int]]("test" -> Map[String, Int]("test2" -> 5))
        val bb = Pickle.intoBytes(testMap)
        assert(Unpickle[Map[String, Map[String, Int]]].fromBytes(bb) == testMap)
      }
    }
    'Set - {
      'empty {
        val bb = Pickle.intoBytes(Set.empty[String])
        assert(bb.limit == 1)
        assert(Unpickle[Set[String]].fromBytes(bb) == Set.empty[String])
      }
      'simple {
        val bb = Pickle.intoBytes(Set(1, 2, -1))
        assert(bb.limit == 1 + 1 + 1 + 2)
        assert(Unpickle[Set[Int]].fromBytes(bb) == Set(1, 2, -1))
      }
    }
    'Array - {
      'empty {
        val bb = Pickle.intoBytes(Array.empty[Int])
        assert(bb.limit == 1)
        assert(Unpickle[Array[Int]].fromBytes(bb) sameElements Array.empty[Int])
      }
      'simple {
        val bb = Pickle.intoBytes(Array(0, 1, -1, -5555))
        assert(bb.limit == 1 + 1 + 1 + 2 + 3)
        assert(Unpickle[Array[Int]].fromBytes(bb) sameElements Array(0, 1, -1, -5555))
      }
      'arrayOfArray {
        val bb = Pickle.intoBytes(Array(Array(0, 0, 0), Array(1, 1, 1)))
        assert(bb.limit == 1 + 4 + 4)
        assert(Unpickle[Array[Array[Int]]].fromBytes(bb).deep == Array(Array(0, 0, 0), Array(1, 1, 1)).deep)
      }
    }
    'ByteBuffer - {
      'small {
        val d = ByteBuffer.allocateDirect(256) // default byte order is Big Endian
        (0 until 256).map(b => d.put(b.toByte))
        d.flip()
        val bb = Pickle.intoBytes(d)
        assert(bb.limit == 2 + 256)
        val r = Unpickle[ByteBuffer].fromBytes(bb)
        assert(r.order() == ByteOrder.BIG_ENDIAN) // check byte order
        assert(r.remaining() == 256)
        assert(r.compareTo(d) == 0)
      }
      'complex {
        val d = Pickle.intoBytes("Testing") // BooPickle output is Little Endian
        val data:(String, ByteBuffer, String) = ("Hello", d, "World")
        val bb = Pickle.intoBytes(data)
        assert(bb.limit == 6 + 9 + 6)
        val r = Unpickle[(String, ByteBuffer, String)].fromBytes(bb)
        val rd = Unpickle[String].fromBytes(r._2)
        assert(r._2.order() == ByteOrder.LITTLE_ENDIAN) // check byte order
        assert(r._1 == data._1)
        assert(r._3 == data._3)
        assert(rd == "Testing")
      }
    }
    'IdentityDeduplication - {
      'Seq - {
        val data = Seq(1, 2, 3, 4, 5)
        val bb = Pickle.intoBytes(Seq(data, data, data))
        assert(bb.limit == 1 + 1 + 5 + 2 * 2) // seq len, first instance, 2x reference
        assert(Unpickle[Seq[Seq[Int]]].fromBytes(bb) == Seq(data, data, data))
      }
    }
    // Storing multiple separate pickles into the same ByteBuffer
    'MultiPickle - {
      'twoStrings {
        val s = Pickle("Hello")
        val bb = s.pickle("World").toByteBuffer
        assert(bb.limit == 6 + 6)
        val state = UnpickleState(bb)
        assert(Unpickle[String].fromState(state) == "Hello")
        assert(Unpickle[String].fromState(state) == "World")
      }
      'stringRef {
        val s = Pickle("Hello")
        val bb = s.pickle("Hello").toByteBuffer
        assert(bb.limit == 6 + 2)
        val state = UnpickleState(bb)
        assert(Unpickle[String].fromState(state) == "Hello")
        assert(Unpickle[String].fromState(state) == "Hello")
      }
    }
    'CustomPickleState - {
      'HeapBuffer {
        val state = new PickleState(new Encoder(new HeapByteBufferProvider))
        val s = state.pickle("Hello")
        val bb = s.toByteBuffer
        assert(bb.limit == 6)
        assert(bb.hasArray)
        val ba = bb.array
        assert(ba(0) == 5)
        assert(Unpickle[String].fromBytes(bb) == "Hello")
      }
      'DirectBuffer {
        val state = new PickleState(new Encoder(new DirectByteBufferProvider))
        val s = state.pickle("Hello")
        val bb = s.toByteBuffer
        assert(bb.limit == 6)
        assert(bb.isDirect)
      }
    }
  }
}
