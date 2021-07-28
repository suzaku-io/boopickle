package boopickle

import boopickle.Default.{generatePickler => _, _}
import scala.collection.immutable._
import utest._

object Scala213PlusTest extends TestSuite {

  override def tests = Tests {

    "Map" - {
      type M = Map[Int, String]
      val m: M = Map(2 -> "two", 4 -> "four")
      val bb   = Pickle.intoBytes(m)
      assert(bb.limit() == 12)
      assert(Unpickle[M].fromBytes(bb) == m)
    }

    "SeqMap" - {
      type M = SeqMap[Int, String]
      val m: M = SeqMap(2 -> "two", 4 -> "four")
      val bb   = Pickle.intoBytes(m)
      assert(bb.limit() == 12)
      assert(Unpickle[M].fromBytes(bb) == m)
    }

    "ListMap" - {
      type M = ListMap[Int, String]
      val m: M = ListMap(2 -> "two", 4 -> "four")
      val bb   = Pickle.intoBytes(m)
      assert(bb.limit() == 12)
      assert(Unpickle[M].fromBytes(bb) == m)
    }

  }
}
