package boopickle

import scala.compiletime._
import scala.deriving._
import scala.quoted._
import scala.language.`3.0`
import scala.reflect.ClassTag

object PicklerMaterializersImpl {

  inline def derive[A <: AnyRef](using m: Mirror.Of[A]): Pickler[A] = {
    lazy val picklers = summonAllPicklers[m.MirroredElemTypes].asInstanceOf[List[Pickler[Any]]]
    inline m match {
      case p: Mirror.ProductOf[A] => deriveProduct(p, picklers)
      case s: Mirror.SumOf[A]     => deriveSum[A](s)
    }
  }

  def deriveProduct[A <: AnyRef](m: Mirror.ProductOf[A], _picklers: => List[Pickler[Any]]): Pickler[A] = {
    lazy val picklers = _picklers.toArray
    new Pickler[A] {
      override def pickle(value: A)(implicit state: PickleState): Unit = {
        val ref = state.identityRefFor(value)
        if (ref.isDefined) {
          state.enc.writeInt(-ref.get)
        } else {
          state.enc.writeInt(0)
          value.asInstanceOf[Product].productIterator.zip(picklers).foreach { case (f, p) =>
            p.pickle(f)
          }
          state.addIdentityRef(value)
        }
      }

      override def unpickle(implicit state: UnpickleState): A = {
        val ic = state.dec.readInt
        if (ic == 0) {
          val value = m.fromProduct(Tuple.fromArray(picklers.map(_.unpickle)))
          state.addIdentityRef(value)
          value
        } else if (ic < 0) {
          state.identityFor[A](-ic)
        } else {
          state.codingError(ic)
        }
      }
    }
  }

  inline def deriveSum[A](m: Mirror.SumOf[A]): Pickler[A] =
    ${ _deriveSum[A]('m) }

  def _deriveSum[A](m: Expr[Mirror.SumOf[A]])(using Quotes, Type[A]): Expr[Pickler[A]] = {
    import quotes.reflect._

    def fields[T <: Tuple](using Type[T]): List[Expr[(Pickler[A], ClassTag[A])]] = {
      Type.of[T] match {
        case '[ h *: tail ] =>
          val p = exprSummonLater[Pickler [h]].asInstanceOf[Expr[Pickler [A]]]
          val c = exprSummonLater[ClassTag[h]].asInstanceOf[Expr[ClassTag[A]]]
          '{ ($p, $c) } :: fields[tail]
        case '[ EmptyTuple ] =>
          Nil
      }
    }

    m match {
      case '{ type t <: Tuple; $x: Mirror.SumOf[A] { type MirroredElemTypes = `t` }} =>
        val fs = fields[t]
        val e = '{ sumTypeHack[A](${ Expr.ofList(fs) }) }
        inlineExpr(e)
    }
  }

  def sumTypeHack[A](fields: List[(Pickler[A], ClassTag[A])]): CompositePickler[A] =
    new CompositePickler[A] {
      for (f <- fields)
        addConcreteType(f._1, f._2)
    }

  // ===================================================================================================================

  def inlineExpr[A](self: Expr[A])(using Quotes, Type[A]): Expr[A] = {
    import quotes.reflect._
    self.asTerm match {
      case _: Inlined => self
      case term       => Inlined(None, Nil, term).asExprOf[A]
    }
  }

  inline def summonAllPicklers[A <: Tuple]: List[Pickler[_]] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (a *: as)  => summonInline[Pickler[a]] :: summonAllPicklers[as]
    }

  inline def summonAllClassTags[A <: Tuple]: List[ClassTag[_]] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (a *: as)  => summonInline[ClassTag[a]] :: summonAllClassTags[as]
    }

  inline def summonLater[A]: A =
    summonInline[A]

  def exprSummonLater[A: Type](using Quotes): Expr[A] =
    '{ summonLater[A] }

}
