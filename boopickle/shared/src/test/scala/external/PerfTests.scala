package external

import java.nio.ByteBuffer

import boopickle.{DecoderSize, EncoderSize, PickleState, UnpickleState}
import boopickle.Default._
import utest._

object PerfTests extends TestSuite {
  def tests = TestSuite {
    'Performance - {
      case class Test(i: Int, s: String)
      // generate data
      val template = (0 until 50000).map(i => Test(i, (i/2).toString * 20))
      val data = (0 until 200000).map(i => template(i % template.size))
      def dedupTest(topic: String, pState: => PickleState, uState: ByteBuffer => UnpickleState) = {
        def testRun = {
          val pickleState = pState
          val start = System.nanoTime()
          val bb = pickleState.pickle(data).toByteBuffer
          val middle = System.nanoTime()
          val unpickleState = uState(bb)
          val uData = unpickleState.unpickle[Seq[Test]]
          val end = System.nanoTime()
          (middle - start, end - middle, uData, bb)
        }
        val (eTime, dTime, uData, bb) = (0 until 3).foldLeft((Long.MaxValue, Long.MaxValue, Seq.empty[Test], null.asInstanceOf[ByteBuffer])) { case ((enc, dec, _, _), idx) =>
          val (e, d, data, bb) = testRun
          (e min enc, d min dec, data, bb)
        }
        println(s"$topic -- Pickle time: ${eTime / 1000}, Unpickle time:  ${dTime / 1000}")
        println(s"Data size ${bb.capacity()}")
        assert(uData == data)
      }
      'Deduplication {
        dedupTest("With dedup", new PickleState(new EncoderSize, true, true), bb => new UnpickleState(new DecoderSize(bb), true, true))
      }
      'NoDeduplication {
        dedupTest("Without dedup", new PickleState(new EncoderSize, false, false), bb => new UnpickleState(new DecoderSize(bb), false, false))
      }
    }
  }
}
