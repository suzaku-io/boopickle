package boopickle

import java.nio.ByteBuffer

import utest._

object CodecTests extends TestSuite {

  /**
    * Helper function to run codec tests.
    *
    * @param data Sequence of data
    * @param w    Writer function
    * @param r    Reader/comparison function
    * @tparam T Data type
    */
  def runCodec[T](data: Seq[T], w: (Encoder, T) => Unit, r: (Decoder, T) => Boolean) = {
    locally {
      val e = new EncoderSize
      data.foreach { x =>
        w(e, x)
      }
      val bb = e.asByteBuffer
      val d  = new DecoderSize(bb)
      data.foreach { x =>
        assert(r(d, x))
      }
    }
    locally {
      val e = new EncoderSpeed
      data.foreach { x =>
        w(e, x)
      }
      val bb = e.asByteBuffer
      val d  = new DecoderSpeed(bb)
      data.foreach { x =>
        assert(r(d, x))
      }
    }
  }

  override def tests = Tests {
    "Byte" - {
      val data = Seq[Byte](0, 1, -128, 127, -1)
      runCodec[Byte](data, (e, x) => { e.writeByte(x); () }, (d, x) => d.readByte == x)
    }

    "Int" - {
      val data = Seq(
        0,
        1,
        -1,
        4095,
        -4096,
        -4097,
        -4098,
        -4099,
        4096,
        4097,
        4098,
        4099,
        1048575,
        1048576,
        -1048576,
        -1048577,
        268435455,
        268435456,
        -268435456,
        -268435457,
        Int.MaxValue,
        Int.MinValue
      )
      runCodec[Int](data, (e, x) => { e.writeInt(x); () }, (d, x) => d.readInt == x)
    }

    "Long" - {
      val data = Seq[Long](0,
                           1,
                           -1,
                           -4096,
                           -4097,
                           4096,
                           1048575,
                           1048576,
                           -1048576,
                           -1048577,
                           Int.MaxValue,
                           Int.MinValue,
                           Int.MaxValue + 1,
                           Int.MinValue - 1,
                           Long.MaxValue,
                           Long.MinValue)
      runCodec[Long](data, (e, x) => { e.writeLong(x); () }, (d, x) => d.readLong == x)
    }

    "Float" - {
      val data =
        Seq[Float](0.0f, 1.0f, -0.5f, Float.MaxValue, Float.MinValue, Float.NegativeInfinity, Float.PositiveInfinity)
      runCodec[Float](data, (e, x) => { e.writeFloat(x); () }, (d, x) => d.readFloat == x)
    }

    "Double" - {
      val data =
        Seq[Double](0.0, 1.0, -0.5, Double.MaxValue, Double.MinValue, Double.NegativeInfinity, Double.PositiveInfinity)
      runCodec[Double](data, (e, x) => { e.writeDouble(x); () }, (d, x) => d.readDouble == x)
    }

    "Char" - {
      val data = Seq[Char](' ', 'A', 'Ö', '叉', '€')
      runCodec[Char](data, (e, x) => { e.writeChar(x); () }, (d, x) => d.readChar == x)
    }

    "String" - {
      val data = Seq[String]("", "A", "叉", "Normal String", "Arabic ڞ", "Complex \uD840\uDC00\uD841\uDDA7")
      runCodec[String](data, (e, x) => { e.writeString(x); () }, (d, x) => d.readString == x)
    }

    "ByteBuffer" - {
      val bb = ByteBuffer.allocateDirect(256)
      for (i <- 0 until 256) bb.put(i.toByte)
      bb.flip
      val e = new EncoderSize
      e.writeByteBuffer(bb)
      val ebb = e.asByteBuffer
      val d   = new DecoderSize(ebb)
      val y   = d.readByteBuffer
      assert(y.compareTo(bb) == 0)
    }

    "ByteArray" - {
      val ba = Seq(Array[Byte](0, 127, -128, -1, 1), Array[Byte]())
      runCodec[Array[Byte]](ba, (e, x) => { e.writeByteArray(x); () }, (d, x) => d.readByteArray().sameElements(x))
    }

    "IntArray" - {
      val ba = Seq(Array[Int](0, Int.MaxValue, Int.MinValue, -1, 1, 256, 65536), Array[Int]())
      runCodec[Array[Int]](ba, (e, x) => { e.writeIntArray(x); () }, (d, x) => d.readIntArray().sameElements(x))
    }

    "FloatArray" - {
      val ba = Seq(Array[Float](0, Float.MaxValue, Float.MinValue, -1, 1, 256.0f, 65536.0f), Array[Float]())
      runCodec[Array[Float]](ba, (e, x) => { e.writeFloatArray(x); () }, (d, x) => d.readFloatArray().sameElements(x))
    }

    "DoubleArray" - {
      val ba = Seq(Array[Double](0, Double.MaxValue, Double.MinValue, -1, 1, 256.0, 65536.0), Array[Double]())
      runCodec[Array[Double]](ba, (e, x) => { e.writeDoubleArray(x); () }, (d, x) => d.readDoubleArray().sameElements(x))
    }

    "StringEncoding" - {
      "all" - {
        val cp    = Array.tabulate[Char](65536)(i => i.toChar)
        val str   = new String(cp)
        val codec = new StringCodecFast {}
        val bb    = ByteBuffer.allocate(cp.length * 3)
        codec.encodeFast(str, bb)
        bb.flip()
        val res = codec.decodeFast(cp.length, bb)
        var i   = 0
        while (i < str.length) {
          assert(res.charAt(i) == str.charAt(i))
          i += 1
        }
      }
      "unicode" - {
        val str   = "\u0c64\u866f\u6a55\ufffd"
        val codec = new StringCodecFast {}
        val bb    = ByteBuffer.allocate(str.length * 3)
        codec.encodeFast(str, bb)
        bb.flip()
        val res = codec.decodeFast(str.length, bb)
        assert(res == str)
      }
    }
  }
}
