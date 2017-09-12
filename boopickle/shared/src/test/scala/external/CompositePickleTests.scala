package external

import utest._
import boopickle.Default._

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

sealed trait Tree

case object Leaf extends Tree

case class Node(value: Int, children: Seq[Tree]) extends Tree

object Tree {
  implicit val treePickler = compositePickler[Tree]
  treePickler.addConcreteType[Node].addConcreteType[Leaf.type]
}

sealed trait Element

sealed trait Document extends Element

sealed trait Attribute extends Element

object Element {
  implicit val documentPickler = compositePickler[Document]
  documentPickler.addConcreteType[WordDocument]

  implicit val attributePickler = compositePickler[Attribute]
  attributePickler.addConcreteType[OwnerAttribute]

  implicit val elementPickler = compositePickler[Element]
  elementPickler.join[Document].join[Attribute]
}

final case class WordDocument(text: String) extends Document

final case class OwnerAttribute(owner: String, parent: Element) extends Attribute

object CompositePickleTests extends TestSuite {
  override def tests = TestSuite {
    'CaseClassHierarchySeq {
      implicit val fruitPickler = compositePickler[Fruit].addConcreteType[Banana].addConcreteType[Kiwi].addConcreteType[Carambola]

      val fruits: Seq[Fruit] = Seq(Kiwi(0.5), Kiwi(0.6), Carambola(5.0), Banana(1.2))
      val bb                 = Pickle.intoBytes(fruits)
      val u                  = Unpickle[Seq[Fruit]].fromBytes(bb)
      assert(u == fruits)
    }
    'CaseClassHierarchy {
      implicit val fruitPickler = compositePickler[Fruit].addConcreteType[Banana].addConcreteType[Kiwi].addConcreteType[Carambola]

      val b  = Banana(1.0)
      val bb = Pickle.intoBytes(b)
      assert(Unpickle[Banana].fromBytes(bb) == b) // This produces Banana
      val bb2 = Pickle.intoBytes(b)
      assert(Unpickle[Fruit].fromBytes(bb2) == null) // This produces null

      // Instead pickle with the parent's type
      val f: Fruit = Banana(1.0)
      val bf       = Pickle.intoBytes(f)
      assert(Unpickle[Fruit].fromBytes(bf) == f) // This produces a Fruit
    }
    'CaseObjects {
      implicit val errorPickler =
        compositePickler[Error].addConcreteType[InvalidName.type].addConcreteType[Unknown.type].addConcreteType[NotFound.type]
      val errors: Map[Error, String] = Map(InvalidName -> "InvalidName", Unknown -> "Unknown", NotFound -> "Not found")
      val bb                         = Pickle.intoBytes(errors)
      val u                          = Unpickle[Map[Error, String]].fromBytes(bb)
      assert(u == errors)
    }
    'Recursive {
      val tree: Tree = Node(1, Seq(Node(2, Seq(Leaf, Node(3, Seq(Leaf, Leaf)), Node(5, Seq(Leaf, Leaf))))))
      val bb         = Pickle.intoBytes(tree)
      val u          = Unpickle[Tree].fromBytes(bb)
      assert(u == tree)
    }
    'Complex {
      val doc        = WordDocument("Testing")
      val q: Element = OwnerAttribute("me", doc)
      val bb         = Pickle.intoBytes(q)
      val u          = Unpickle[Element].fromBytes(bb)
      assert(u == q)
    }
    'Transformers {
      implicit val datePickler = transformPickler((t: Long) => new java.util.Date(t))(_.getTime)
      val date                 = new java.util.Date()
      val bb                   = Pickle.intoBytes(date)
      val d                    = Unpickle[java.util.Date].fromBytes(bb)
      assert(d == date)
    }
    'Exceptions {
      implicit val exPickler = exceptionPickler

      val exs: Seq[Throwable] = Seq(
        new NullPointerException("Noooo!"),
        new IllegalArgumentException("Your argument is not valid"),
        new ArrayIndexOutOfBoundsException("There's no such index as 42 here!")
      )
      val bb = Pickle.intoBytes(exs)
      val e  = Unpickle[Seq[Throwable]].fromBytes(bb)
      assert(e.zip(exs).forall(x => x._1.getMessage == x._2.getMessage && x._1.getClass == x._2.getClass))
    }
    'AddClassTwice {
      intercept[IllegalArgumentException] {
        compositePickler[Fruit].addConcreteType[Banana].addConcreteType[Banana]
      }
    }
  }
}
