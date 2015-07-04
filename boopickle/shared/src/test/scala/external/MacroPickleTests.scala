package external

import boopickle.Default._
import utest._

object MacroPickleTests extends TestSuite {

  case class Test1(i: Int, x: String)

  case class Test2(i: Int, next: Option[Test2], l: Map[String, String] = Map.empty)

  case object TestO

  sealed trait MyTrait

  case class TT1(i: Int) extends MyTrait

  sealed trait DeepTrait extends MyTrait

  case class TT2(s: String, next: MyTrait) extends DeepTrait

  class TT3(val i: Int, val s: String) extends DeepTrait {
    // a normal class requires an equals method to work properly
    override def equals(obj: scala.Any): Boolean = obj match {
      case t: TT3 => i == t.i && s == t.s
      case _ => false
    }
  }

  object TT3 {
  }

  object MyTrait {
    // a pickler for non-case classes cannot be automatically generated, so use the transform pickler
    implicit val pickler3 = transformPickler[TT3, (Int, String)]((t) => (t.i, t.s), (t) => new TT3(t._1, t._2))
    implicit val pickler = generatePickler[MyTrait]
  }

  case class A(fills: List[B])

  case class B(stops: List[(Double, Double)])

  sealed trait A1Trait[T]
  case class A1[T](i: T) extends A1Trait[T]

  override def tests = TestSuite {
    // must import pickler from the companion object, otherwise scalac will try to use a macro to generate it
    import MyTrait._
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
        val t = List(Test2(1, Some(Test2(2, Some(Test2(3, None))))))
        val bb = Pickle.intoBytes(t)
        assert(bb.limit == 16)
        val u = Unpickle[List[Test2]].fromBytes(bb)
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
        // the same test code twice, to check that additional .class files are not generated for the MyTrait pickler
        val t: Seq[MyTrait] = Seq(TT1(5), TT2("five", TT2("six", new TT3(42, "fortytwo"))))
        val bb = Pickle.intoBytes(t)
        val u = Unpickle[Seq[MyTrait]].fromBytes(bb)
        assert(u == t)
      }
      'CaseTupleList {
        // this won't compile due to "diverging implicits"
        // val x = A(List(B(List(Tuple2(2.0, 1.0)))))
        // val bb = Pickle.intoBytes(x)
        // val u = Unpickle[A].fromBytes(bb)
        // assert(x == u)
      }
      'CaseTupleList2 {
        implicit val bPickler = generatePickler[B]
        val x = A(List(B(List((2.0, 3.0)))))
        val bb = Pickle.intoBytes(x)
        val u = Unpickle[A].fromBytes(bb)
        assert(x == u)
      }
      'CaseTupleList3 {
        val x = List(B(List((2.0, 3.0))))
        val bb = Pickle.intoBytes(x)
        val u = Unpickle[List[B]].fromBytes(bb)
        assert(x == u)
      }
      'CaseGenericTraitAndCaseclass {
        val x: A1Trait[Int] = A1[Int](2)
        val bb = Pickle.intoBytes(x)
        val u = Unpickle[A1Trait[Int]].fromBytes(bb)
        assert(x == u)
      }
      'CaseGenericTraitAndCaseclass2 {
        val x: A1Trait[Double] = A1[Double](2.0)
        val bb = Pickle.intoBytes(x)
        val u = Unpickle[A1Trait[Double]].fromBytes(bb)
        assert(x == u)
      }
    }
  }
}

