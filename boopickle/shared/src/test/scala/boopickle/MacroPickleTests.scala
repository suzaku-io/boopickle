package boopickle

import utest._

object MacroPickleTests extends TestSuite {

  case class Test1(i: Int, x: String)

  case class Test2(i: Int, next: Option[Test2])

  case object TestO

  override def tests = TestSuite {
    'CaseClasses - {
      'Case1 {
        val bb = Pickle.intoBytes(Test1(5, "Hello!"))
        assert(bb.limit == 1 + 1 + 7)
        assert(Unpickle[Test1].fromBytes(bb) == Test1(5, "Hello!"))
      }
      'SeqCase {
        val t = Test1(99, "Hello!")
        val s = Seq(t, t, t)
        val bb = Pickle.intoBytes(s)
        assert(bb.limit == 1 + 1 + 1 + 7 + 2 * 2)
        val u = Unpickle[Seq[Test1]].fromBytes(bb)
        assert(u == s)
      }
      'Recursive {
        val t = Test2(1, Some(Test2(2, Some(Test2(3, None)))))
        val bb = Pickle.intoBytes(t)
        assert(bb.limit == 10)
        val u = Unpickle[Test2].fromBytes(bb)
        assert(u == t)
      }
      'CaseObject {
        val bb = Pickle.intoBytes(TestO)
        // yea, pickling a case object takes no space at all :)
        assert(bb.limit == 0)
        val u = Unpickle[TestO.type].fromBytes(bb)
        assert(u == TestO)
      }
    }
  }
}

