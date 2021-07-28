package boopickle

import scala.compiletime._
import scala.deriving._
import scala.quoted._
import scala.language.`3.0`
import scala.reflect.ClassTag

object PicklerMaterializersImpl {

  inline def derive[A]: Pickler[A] =
    ${ _derive[A] }

  def _derive[A](using Quotes, Type[A]): Expr[Pickler[A]] = {
    import quotes.reflect._

    Expr.summon[Mirror.Of[A]] match {
      case Some('{ $p: Mirror.ProductOf[A] }) => deriveProduct[A](p)
      case Some('{ $s: Mirror.SumOf[A]     }) => deriveSum[A](s)
      case _                                  => deriveOther[A]
    }
  }

  def deriveProduct[A](m: Expr[Mirror.ProductOf[A]])(using Quotes, Type[A]): Expr[Pickler[A]] = {
    import quotes.reflect._

    // println("------------------------------------------------------------------------------")
    // println(Position.ofMacroExpansion)
    // println(Type.show[A])
    // println()

    m match {

      case '{ $x: Mirror.ProductOf[A] { type MirroredElemTypes = EmptyTuple }} =>
        val a = '{ $x.fromProduct(EmptyTuple) }
        '{ ConstPickler($a) }

      case '{ $x: Mirror.ProductOf[A] { type MirroredElemTypes = t *: EmptyTuple }} =>
        val pickler = exprSummonLater[Pickler[t]]
        '{ $pickler.xmap[A](
              v => $x.fromProduct(Tuple1(v)))(
              a => a.asInstanceOf[Product].productElement(0).asInstanceOf[t])
        }

      case '{ type p <: AnyRef; type t <: Tuple; $x: Mirror.ProductOf[`p`] { type MirroredElemTypes = `t` }} =>
        lazy val picklerExprs = summonAllPicklers[t]
        lazy val picklers = Expr.ofList(picklerExprs)
        val result: Expr[Pickler[p]] = '{ deriveAnyRefProduct[p]($x, $picklers) }
        result.asInstanceOf[Expr[Pickler[A]]]
    }
  }

  def deriveAnyRefProduct[A <: AnyRef](m: Mirror.ProductOf[A], _picklers: => List[Pickler[_]]): Pickler[A] = {
    lazy val picklers = _picklers.asInstanceOf[List[Pickler[Any]]]
    lazy val picklerCount = picklers.size
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
          val a = new Array[Any](picklerCount)
          var i = 0
          for (p <- picklers) {
            a(i) = p.unpickle
            i += 1
          }
          val value = m.fromProduct(Tuple.fromArray(a))
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

  def deriveSum[A](m: Expr[Mirror.SumOf[A]])(using Quotes, Type[A]): Expr[Pickler[A]] = {
    import quotes.reflect._

    def fields[T <: Tuple](using Type[T]): List[Expr[(Pickler[_], ClassTag[_])]] = {
      Type.of[T] match {
        case '[ h *: tail ] =>
          // println("  - " + Type.show[h])
          val p = exprSummonLater[Pickler [h]]
          val c = exprSummonLater[ClassTag[h]]
          '{ ($p, $c) } :: fields[tail]
        case '[ EmptyTuple ] =>
          Nil
      }
    }

    m match {
      case '{ type t <: Tuple; $x: Mirror.SumOf[A] { type MirroredElemTypes = `t` }} =>
        // println("------------------------------------------------------------------------------")
        // println(Position.ofMacroExpansion)
        // println(Type.show[t])
        // println()
        val fs = fields[t]
        // println()
        val e = '{ sumTypeHack[A](${ Expr.ofList(fs) }) }
        inlineExpr(e)
    }
  }

  def sumTypeHack[A](fields: List[(Pickler[_], ClassTag[_])]): CompositePickler[A] =
    new CompositePickler[A] {
      for (f <- fields) {
        val p = f._1.asInstanceOf[Pickler[A]]
        val c = f._2.asInstanceOf[ClassTag[A]]
        addConcreteType(p, c)
      }
    }

  def deriveOther[A](using Quotes, Type[A]): Expr[Pickler[A]] = {
    import quotes.reflect._

    val A = TypeRepr.of[A]

    A.classSymbol match {
      case Some(sym) if sym.flags.is(Flags.Case) =>
        (sym.caseFields, sym.companionModule.memberMethod("apply")) match {
          case (field :: Nil, apply :: Nil) =>
            TypeRepr.of[A].memberType(field).asType match {
              case '[ t ] =>
                lazy val pickler = exprSummonLater[Pickler[t]]
                apply.tree match {
                  case DefDef(_, (p :: Nil) :: Nil, _, _) =>
                    def build(e: Expr[t]): Expr[A] = Apply(Ref(apply), e.asTerm :: Nil).asExprOf[A]
                    def access(e: Expr[A]): Expr[t] = Select(e.asTerm, field).asExprOf[t]
                    return '{ $pickler.xmap[A](b => ${build('b)})(a => ${access('a)}) }
                  case _ =>
                }
              }
          case _ =>
        }
      case _ =>
    }

    report.throwError(s"Don't know how to generate a Pickler[${Type.show[A]}]")
  }

  // ===================================================================================================================

  def inlineExpr[A](self: Expr[A])(using Quotes, Type[A]): Expr[A] = {
    import quotes.reflect._
    self.asTerm match {
      case _: Inlined => self
      case term       => Inlined(None, Nil, term).asExprOf[A]
    }
  }

  def summonAllPicklers[A <: Tuple](using Quotes, Type[A]): List[Expr[Pickler[_]]] = {
    import quotes.reflect._
    Type.of[A] match {
      case '[ EmptyTuple ] => Nil
      case '[ a *: as ] => exprSummonLater[Pickler[a]] :: summonAllPicklers[as]
    }
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
