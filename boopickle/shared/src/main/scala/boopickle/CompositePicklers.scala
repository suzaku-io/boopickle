package boopickle

import scala.reflect.ClassTag

/**
 * Encodes a class belonging to a type hierarchy. Type is identified by the index in the `picklers` sequence, so care
 * must be taken to ensure picklers are added in the same order.
 */
case class CompositePickler[A <: AnyRef](var picklers: Vector[(String, Pickler[_])] = Vector()) extends Pickler[A] {

  import Constants._
  import Default.stringPickler

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

  override def unpickle(implicit state: UnpickleState): A = {
    val idx = state.dec.readInt
    if (idx == CompositeNull)
      null.asInstanceOf[A]
    else {
      if (idx < 0 || idx > picklers.length)
        throw new IllegalStateException(s"Index $idx is not defined in this CompositePickler")
      picklers(idx - 1)._2.asInstanceOf[Pickler[A]].unpickle
    }
  }

  def addConcreteType[B <: A](implicit p: Pickler[B], tag: ClassTag[B]): CompositePickler[A] = {
    picklers :+= (tag.runtimeClass.getName -> p)
    this
  }

  def addTransform[B <: A, C](transformTo: (B) => C, transformFrom: (C) => B)(implicit p: Pickler[C], tag: ClassTag[B]) = {
    val pickler = p.xmap(transformFrom)(transformTo)
    picklers :+= (tag.runtimeClass.getName -> pickler)
    this
  }

  def addException[B <: A with Throwable](ctor: (String) => B)(implicit tag: ClassTag[B]) = {
    val pickler = new Pickler[B] {
      override def pickle(ex: B)(implicit state: PickleState): Unit = {
        state.pickle(ex.getMessage)
      }
      override def unpickle(implicit state: UnpickleState): B = {
        ctor(state.unpickle[String])
      }
    }
    picklers :+= (tag.runtimeClass.getName -> pickler)
    this
  }

  def join[B <: A](implicit cp: CompositePickler[B]) = {
    picklers ++= cp.picklers
    this
  }
}

object CompositePickler {
  def apply[A <: AnyRef] = new CompositePickler[A]()
}

object ExceptionPickler {
  def empty = CompositePickler[Throwable]
  // generate base exception picklers
  private lazy val basePicklers = CompositePickler[Throwable].
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
  /**
   * Provides simple (message only) pickling of most common Java/Scala exception types. This can be used
   * as a base for adding custom exception types.
   */
  def base = CompositePickler[Throwable](basePicklers.picklers)
}

