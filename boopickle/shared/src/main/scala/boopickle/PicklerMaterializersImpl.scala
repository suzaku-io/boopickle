package boopickle

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object PicklerMaterializersImpl {
  def materializePickler[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Pickler[T]] = {
    import c.universe._

    val tpe = weakTypeOf[T]

    if (!tpe.typeSymbol.isClass)
      throw new RuntimeException("Enclosure: " + c.enclosingPosition.toString)

    val sym = tpe.typeSymbol.asClass

    if (!sym.isCaseClass) {
      c.error(c.enclosingPosition,
        s"Cannot materialize pickler for non-case class: ${sym.fullName}")
      return c.Expr[Pickler[T]](q"null")
    }

    val pickleLogic = if (sym.isModuleClass) {
      // no need to write anything for case objects
      q"""()"""
    } else {
      val accessors = (tpe.decls collect {
        case acc: MethodSymbol if acc.isCaseAccessor => acc
      }).toList

      val pickleFields = for {
        accessor <- accessors
      } yield
          q"""state.pickle(value.${accessor.name})"""

      q"""
          state.identityRefFor(value) match {
            case Some(idx) =>
              state.enc.writeInt(-idx)
            case None =>
              state.enc.writeInt(0)
              ..$pickleFields
              state.addIdentityRef(value)
          }
        """
    }
    val name = TermName(c.freshName("GenPickler"))

    val result = q"""
      implicit object $name extends boopickle.Pickler[$tpe] {
        import boopickle._
        override def pickle(value: $tpe)(implicit state: PickleState): Unit = $pickleLogic
      }
      $name
    """

    c.Expr[Pickler[T]](result)
  }

  def materializeUnpickler[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Unpickler[T]] = {
    import c.universe._

    val tpe = weakTypeOf[T]
    val sym = tpe.typeSymbol.asClass

    if (!sym.isCaseClass) {
      c.error(c.enclosingPosition,
        s"Cannot materialize pickler for non-case class: ${sym.fullName}")
      return c.Expr[Unpickler[T]](q"null")
    }

    val unpickleLogic = if (sym.isModuleClass) {
      c.parse(sym.fullName)
    } else {
      val accessors = tpe.decls.collect {
        case acc: MethodSymbol if acc.isCaseAccessor => acc
      }.toList

      val unpickledFields = for {
        accessor <- accessors
      } yield {
          val fieldTpe = accessor.typeSignatureIn(tpe)
          q"""state.unpickle[$fieldTpe]"""
        }
      q"""
          state.dec.readIntCode match {
            case Right(0) =>
              val value = new $tpe(..$unpickledFields)
              state.addIdentityRef(value)
              value
            case Right(idx) if idx < 0 =>
              state.identityFor[$tpe](-idx)
            case _ =>
              throw new IllegalArgumentException("Unknown object coding")
          }
        """
    }

    val name = TermName(c.freshName("GenUnpickler"))

    val result = q"""
      implicit object $name extends boopickle.Unpickler[$tpe] {
        import boopickle._
        override def unpickle(implicit state: UnpickleState): $tpe = { $unpickleLogic }
      }
      $name
    """

    c.Expr[Unpickler[T]](result)
  }
}