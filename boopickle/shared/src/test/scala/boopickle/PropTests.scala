package boopickle

import japgolly.nyaya._
import japgolly.nyaya.test._
import japgolly.nyaya.test.PropTest._
import scalaz.Equal
import scalaz.std.AllInstances._
import utest._

object PropTests extends TestSuite {

  // TODO String tests fail.
  // TODO CompositePickler doesn't handle recursive data types.

  def prop[A: Equal: Pickler: Unpickler] =
    Prop.equalSelf[A]("decode.encode = id",
      a => {
        val b = Pickle.intoBytes(a)
        Unpickle[A].fromBytes(b)
      })

  def debugStrProp =
    Prop.equalSelf[String]("decode.encode = id",
      a => {
        val a2 = Unpickle[String].fromBytes(Pickle.intoBytes(a))
        if (a != a2) {
          println()
          println("Before: " + a .toCharArray.map(_.toInt).mkString(","))
          println(" After: " + a2.toCharArray.map(_.toInt).mkString(","))
          println()
        }
        a2
      })

  sealed trait ADT
  object ADT {
    case class A(i: Int) extends ADT
    case class B(a: Char, b: Long, c: Option[Char]) extends ADT
    case object C extends ADT
    case class D() extends ADT
//    case class R(child: ADT) extends ADT
    implicit val equality: Equal[ADT] = Equal.equalA
    implicit val codec = CompositePickler[ADT].concreteType[A].concreteType[B].concreteType[C.type].concreteType[D] //.concreteType[R]
  }
  val genADT: Gen[ADT] = {
    import ADT._
    import Gen.Covariance._
    val ga = Gen.int map A
    val gb = Gen.apply3(B)(Gen.char, Gen.long, Gen.char.option)
    val gc = Gen.insert[C.type](C)
    val gd = Gen.insert(D())
//    lazy val gr: Gen[ADT.R] = g map R
//    lazy val g: Gen[ADT] = Gen.oneofG(ga, gb, gc, gd, gr)
    lazy val g: Gen[ADT] = Gen.oneofG(ga, gb, gc, gd)
    g
  }

  override def tests = TestSuite {
    'boolean - Domain.boolean.mustProve  (prop)
    'byte    - Domain.byte   .mustProve  (prop)
    'int     - Gen.int       .mustSatisfy(prop)
    'long    - Gen.long      .mustSatisfy(prop)
    'char    - Gen.char      .mustSatisfy(prop)
    // 'string  - Gen.string    .mustSatisfy(debugStrProp)
    'float   - Gen.float     .mustSatisfy(prop)
    'double  - Gen.double    .mustSatisfy(prop)

    'adt - genADT.mustSatisfy(prop)

    'option - Domain.byte.option      .mustProve(prop)
    'list   - Gen.int.list            .mustSatisfy(prop)
    'map    - Gen.int.mapTo(Gen.char) .mustSatisfy(prop)
    'either - Gen.int.either(Gen.char).mustSatisfy(prop)
  }
}
