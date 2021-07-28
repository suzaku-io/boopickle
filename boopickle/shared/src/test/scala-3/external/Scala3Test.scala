package external

import boopickle.Default.{generatePickler => _, _}
import utest._

object Scala3Test extends TestSuite {

  case class Test1(i: Int, x: String) derives Pickler

  override def tests = Tests {

    "derives" - {
      val bb = Pickle.intoBytes(Test1(5, "Hello!"))
      assert(bb.limit() == 1 + 1 + 7)
      assert(Unpickle[Test1].fromBytes(bb) == Test1(5, "Hello!"))
    }

  }
}
