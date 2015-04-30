package boopickle

import java.nio.ByteBuffer

import utest._

object CodecTests extends TestSuite {
  /**
   * Helper function to run codec tests.
   *
   * @param data Sequence of data
   * @param w Writer function
   * @param r Reader/comparison function
   * @tparam T Data type
   */
  def runCodec[T](data: Seq[T], w: (Encoder, T) => Unit, r: (Decoder, T) => Boolean) = {
    val e = new Encoder
    data.foreach { x => w(e, x) }
    val bb = e.asByteBuffer
    val d = new Decoder(bb)
    data.foreach { x => assert(r(d, x)) }
  }

  override def tests = TestSuite {
    'Byte - {
      val data = Seq[Byte](0, 1, -128, 127, -1)
      runCodec[Byte](data, (e, x) => e.writeByte(x), (d, x) => d.readByte == x)
    }

    'Int - {
      val data = Seq(0, 1, -1, 4095, -4096, -4097, -4098, -4099, 4096, 4097, 4098, 4099, 1048575, 1048576, -1048576, -1048577, 268435455, 268435456, -268435456, -268435457, Int.MaxValue, Int.MinValue)
      runCodec[Int](data, (e, x) => e.writeInt(x), (d, x) => d.readInt == x)
    }

    'Long - {
      val data = Seq[Long](0, 1, -1, -4096, -4097, 4096, 1048575, 1048576, -1048576, -1048577, Int.MaxValue, Int.MinValue,
        Int.MaxValue + 1, Int.MinValue - 1, Long.MaxValue, Long.MinValue)
      runCodec[Long](data, (e, x) => e.writeLong(x), (d, x) => d.readLong == x)
    }

    'Float - {
      val data = Seq[Float](0.0f, 1.0f, -0.5f, Float.MaxValue, Float.MinValue, Float.NegativeInfinity, Float.PositiveInfinity)
      runCodec[Float](data, (e, x) => e.writeFloat(x), (d, x) => d.readFloat == x)
    }

    'Double - {
      val data = Seq[Double](0.0, 1.0, -0.5, Double.MaxValue, Double.MinValue, Double.NegativeInfinity, Double.PositiveInfinity)
      runCodec[Double](data, (e, x) => e.writeDouble(x), (d, x) => d.readDouble == x)
    }

    'Char - {
      val data = Seq[Char](' ', 'A', 'Ö', '叉', '€')
      runCodec[Char](data, (e, x) => e.writeChar(x), (d, x) => d.readChar == x)
    }

    'String - {
      val data = Seq[String]("", "A", "叉", "Normal String", "Arabic ڞ", "Complex \uD840\uDC00\uD841\uDDA7")
      runCodec[String](data, (e, x) => e.writeString(x), (d, x) => d.readString == x)
    }

    'ByteBuffer - {
      val bb = ByteBuffer.allocateDirect(256)
      for (i <- 0 until 256) bb.put(i.toByte)
      bb.flip
      val e = new Encoder
      e.writeByteBuffer(bb)
      val ebb = e.asByteBuffer
      val d = new Decoder(ebb)
      val y = d.readByteBuffer
      assert(y.compareTo(bb) == 0)
    }
  }
}
