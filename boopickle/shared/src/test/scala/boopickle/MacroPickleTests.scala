package boopickle

import utest._

object MacroPickleTests extends TestSuite {

  case class Test1(i: Int, x: String)

  case class Test2(i: Int, next: Option[Test2])

  case object TestO

  sealed trait MyTrait

  case class TT1(i: Int) extends MyTrait

  sealed trait DeepTrait extends MyTrait

  case class TT2(s: String, next: MyTrait) extends DeepTrait

  class TT3(val i: Int, val s: String) extends DeepTrait {
    // a normal class requires an equals method to work properly
    override def equals(obj: scala.Any): Boolean = obj match {
      case t:TT3 => i == t.i && s == t.s
      case _ => false
    }
  }

  object TT3 {
    // a piclker for non-case classes cannot be automatically generated, so use the transform pickler
    implicit val pickler = TransformPickler[TT3, (Int, String)]((t) => (t.i, t.s), (t) => new TT3(t._1, t._2))
  }

  override def tests = TestSuite {
/*
    implicit val pickler = Pickler.materializePickler[MyTrait]
    implicit val unpickler = Unpickler.materializeUnpickler[MyTrait]
*/

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
      'Trait {
        val t: Seq[MyTrait] = Seq(TT1(5), TT2("five", TT2("six", new TT3(42, "fortytwo"))))
        val bb = Pickle.intoBytes(t)
        val u = Unpickle[Seq[MyTrait]].fromBytes(bb)
        assert(u == t)
      }
      'TraitToo {
        // again the same test code, to check that additional .class files are generated for the pickler
        val t: Seq[MyTrait] = Seq(TT1(5), TT2("five", TT2("six", new TT3(42, "fortytwo"))))
        val bb = Pickle.intoBytes(t)
        val u = Unpickle[Seq[MyTrait]].fromBytes(bb)
        assert(u == t)
      }
    }
  }
}

