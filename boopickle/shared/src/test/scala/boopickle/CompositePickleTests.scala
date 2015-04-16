package boopickle

import utest._

trait Fruit {
  val weight: Double
  def color: String
}

case class Banana(weight: Double) extends Fruit {
  def color = "yellow"
}

case class Kiwi(weight: Double) extends Fruit {
  def color = "brown"
}

case class Carambola(weight: Double) extends Fruit {
  def color = "yellow"
}

sealed trait Error
case object InvalidName extends Error
case object Unknown extends Error
case object NotFound extends Error

object CompositePickleTests extends TestSuite {
  override def tests = TestSuite {
    'CaseClassHierarchy {
      implicit val fruitPickler = CompositePickler[Fruit].concreteType[Banana].concreteType[Kiwi].concreteType[Carambola]

      val fruits: Seq[Fruit] = Seq(Kiwi(0.5), Kiwi(0.6), Carambola(5.0), Banana(1.2))
      val bb = Pickle.intoBytes(fruits)
      val u = Unpickle[Seq[Fruit]].fromBytes(bb)
      assert(u == fruits)
    }
    'CaseObjects {
      implicit val errorPickler = CompositePickler[Error].concreteType[InvalidName.type].concreteType[Unknown.type].concreteType[NotFound.type]
      val errors:Map[Error, String] = Map(InvalidName -> "InvalidName", Unknown -> "Unknown", NotFound -> "Not found" )
      val bb = Pickle.intoBytes(errors)
      val u = Unpickle[Map[Error, String]].fromBytes(bb)
      assert(u == errors)
    }
  }
}
