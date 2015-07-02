package boopickle

import scala.language.experimental.macros
import scala.reflect.ClassTag

/**
 * Encodes a class belonging to a type hierarchy. Type is identified by the index in the `picklers` sequence, so care
 * must be taken to ensure picklers are added in the same order.
 */
case class CompositePickler[A <: AnyRef](var picklers: Vector[(String, Pickler[_])] = Vector()) extends Pickler[A] {

  import Constants._
  import Default.StringPickler

  override def pickle(obj: A)(implicit state: PickleState): Unit = {
    if (obj == null) {
      state.enc.writeInt(CompositeNull)
    } else {
      val name = obj.getClass.getName
      val idx = picklers.indexWhere(_._1 == name)
      if (idx == -1)
        throw new IllegalArgumentException(s"CompositePickler doesn't know class '$name'. Known classes: ${picklers.map(_._1).mkString(", ")}")
      state.enc.writeInt(idx + 1)
      picklers(idx)._2.asInstanceOf[Pickler[A]].pickle(obj)
    }
  }

  def addConcreteType[B <: A](implicit p: Pickler[B], tag: ClassTag[B]): CompositePickler[A] = {
    picklers :+= (tag.runtimeClass.getName -> p)
    this
  }

  def addTransform[B <: A, C](transformTo: (B) => C)(implicit p: Pickler[C], tag: ClassTag[B]) = {
    val pickler = new Pickler[B] {
      override def pickle(obj: B)(implicit state: PickleState): Unit = {
        p.pickle(transformTo(obj))
      }
    }
    picklers :+= (tag.runtimeClass.getName -> pickler)
    this
  }

  def addException[B <: Exception](implicit tag: ClassTag[B]) = {
    val pickler = new Pickler[B] {
      override def pickle(obj: B)(implicit state: PickleState): Unit = {
        state.pickle(obj.getMessage)
      }
    }
    picklers :+= (tag.runtimeClass.getName -> pickler)
  }

  def join[B <: A](implicit cp: CompositePickler[B]) = {
    picklers ++= cp.picklers
    this
  }
}

/**
 * Decodes a class belonging to a type hierarchy. Type is identified by the index in the `unpicklers` sequence, so care
 * must be taken to ensure unpicklers are added in the same order.
 */
case class CompositeUnpickler[A <: AnyRef](var unpicklers: Vector[(String, Unpickler[_])] = Vector()) extends Unpickler[A] {

  import Constants._
  import Default.StringUnpickler

  override def unpickle(implicit state: UnpickleState): A = {
    val idx = state.dec.readInt
    if (idx == CompositeNull)
      null.asInstanceOf[A]
    else {
      if (idx < 0 || idx > unpicklers.length)
        throw new IllegalStateException(s"Index $idx is not defined in this CompositeUnpickler")
      unpicklers(idx - 1)._2.asInstanceOf[Unpickler[A]].unpickle
    }
  }

  def addConcreteType[B <: A](implicit p: Unpickler[B], tag: ClassTag[B]): CompositeUnpickler[A] = {
    unpicklers :+= (tag.runtimeClass.getName -> p)
    this
  }

  def addTransform[B <: A, C](transformFrom: (C) => B)(implicit up: Unpickler[C], tag: ClassTag[B]) = {
    val unpickler = new Unpickler[B] {
      override def unpickle(implicit state: UnpickleState): B = {
        transformFrom(up.unpickle(state))
      }
    }
    unpicklers :+= (tag.runtimeClass.getName -> unpickler)
    this
  }

  def addException[B <: Exception](ctor: (String) => B)(implicit tag: ClassTag[B]) = {
    val unpickler = new Unpickler[B] {
      override def unpickle(implicit state: UnpickleState): B = {
        ctor(state.unpickle[String])
      }
    }
    unpicklers :+= (tag.runtimeClass.getName -> unpickler)
    this
  }

  def join[B <: A](implicit cu: CompositeUnpickler[B]) = {
    unpicklers ++= cu.unpicklers
    this
  }
}

object CompositePickler {
  def apply[A <: AnyRef] = new PicklerPair[A]()
}

object ExceptionPickler {
  def empty = CompositePickler[Throwable]
  // generate base exception picklers
  private lazy val (basePicklers, baseUnpicklers) = {
    val p = CompositePickler[Throwable].
      addException[Exception](m => new Exception(m)).
      addException[RuntimeException](m => new RuntimeException(m)).
      addException[MatchError](m => new MatchError(m)).
      addException[UninitializedError](_ => new UninitializedError).
      addException[UninitializedFieldError](m => new UninitializedFieldError(m)).
      addException[NullPointerException](m => new NullPointerException(m)).
      addException[ClassCastException](m => new ClassCastException(m)).
      addException[IndexOutOfBoundsException](m => new IndexOutOfBoundsException(m)).
      addException[ArrayIndexOutOfBoundsException](m => new ArrayIndexOutOfBoundsException(m)).
      addException[StringIndexOutOfBoundsException](m => new StringIndexOutOfBoundsException(m)).
      addException[UnsupportedOperationException](m => new UnsupportedOperationException(m)).
      addException[IllegalArgumentException](m => new IllegalArgumentException(m)).
      addException[IllegalStateException](m => new IllegalStateException(m)).
      addException[NoSuchElementException](m => new NoSuchElementException(m)).
      addException[NumberFormatException](m => new NumberFormatException(m)).
      addException[ArithmeticException](m => new ArithmeticException(m)).
      addException[InterruptedException](m => new InterruptedException(m))
    (p.pickler.picklers, p.unpickler.unpicklers)
  }
  /**
   * Provides simple (message only) pickling of most common Java/Scala exception types. This can be used
   * as a base for adding custom exception types.
   */
  def base = PicklerPair[Throwable](new CompositePickler(basePicklers), new CompositeUnpickler(baseUnpicklers))
}

/**
 * Helper for registration of Pickler[B]/Unpickler[B] pairs
 */
case class PicklerPair[A <: AnyRef](pickler: CompositePickler[A] = new CompositePickler[A](),
                                    unpickler: CompositeUnpickler[A] = new CompositeUnpickler[A]()) {

  def addConcreteType[B <: A](implicit p: Pickler[B], u: Unpickler[B], tag: ClassTag[B]) = {
    pickler.addConcreteType[B]
    unpickler.addConcreteType[B]
    this
  }

  def addTransform[B <: A, C](transformTo: (B) => C, transformFrom: (C) => B)(implicit p: Pickler[C], up: Unpickler[C], tag: ClassTag[B]) = {
    pickler.addTransform[B, C](transformTo)
    unpickler.addTransform[B, C](transformFrom)
    this
  }

  def addException[B <: Exception](ctor: (String) => B)(implicit tag: ClassTag[B]) = {
    pickler.addException[B]
    unpickler.addException[B](ctor)
    this
  }

  def join[B <: A](implicit pp: PicklerPair[B]) = {
    pickler.join(pp.pickler)
    unpickler.join(pp.unpickler)
    this
  }
}

/**
 * Create a transforming pickler that takes an object of type `A` and transforms it into `B`, which is then pickled.
 * Similarly a `B` is unpickled and then transformed back into `A`.
 *
 * This allows for easy creation of picklers for (relatively) simple classes. For example
 * {{{
 *   // transform Date into Long and back
 *   implicit val datePickler = TransformPickler[java.util.Date, Long](
 *     _.getTime,
 *     t => new java.util.Date(t))
 * }}}
 *
 * @param transformTo Function that takes `A` and transforms it into `B`
 * @param transformFrom Function that takes `B` and transforms it into `A`
 * @tparam A Type of the original object
 * @tparam B Type for the object used in pickling
 */
case class TransformPickler[A <: AnyRef, B](transformTo: (A) => B, transformFrom: (B) => A)(implicit p: Pickler[B], up: Unpickler[B]) {
  val pickler = new Pickler[A] {
    override def pickle(obj: A)(implicit state: PickleState): Unit = {
      p.pickle(transformTo(obj))
    }
  }
  val unpickler = new Unpickler[A] {
    override def unpickle(implicit state: UnpickleState): A = {
      transformFrom(up.unpickle(state))
    }
  }
}
