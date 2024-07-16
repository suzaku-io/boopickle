package boopickle

import java.nio.ByteBuffer

import utest._

import boopickle.DefaultBasic._

object StringCodecTests extends TestSuite {

  override def tests = Tests {
    "LargeString" - {
      // test data
      val data = new String(Array.fill[Byte](400000)('A'))
      // create encoded strings of various lengths
      def createBB(size: Int) = Pickle.intoBytes(data.substring(0, size))

      val sizes = Seq(1000, 4095, 4096, 4097, 8192, 70000, 200000, 400000)
      val bufs  = sizes.map(createBB)

      val strings = bufs.map(b => Unpickle[String].fromBytes(b))
      sizes.zip(strings).foreach { case (size, str) =>
        assert(str.length == size)
      }
    }
  }
}
