package boopickle

import java.nio.ByteBuffer

import scala.util.Random

import external.Banana
import utest._
import boopickle.Default._

object BufferProviderTests extends TestSuite {

  override def tests = Tests {
    'asByteBuffersProperOrder {

      val input: Seq[Banana] = Iterator.tabulate(1000)(_ => Banana(Random.nextDouble)).toVector
      val bbs = Pickle.intoByteBuffers(input)
      assert(bbs.size > 1)

      val mergedBb = ByteBuffer.allocate(bbs.map(_.remaining).sum)
      bbs.foreach(mergedBb.put)
      mergedBb.flip()

      val output = Unpickle[Seq[Banana]].fromBytes(mergedBb)
      assert(output == input)
    }
  }
}
