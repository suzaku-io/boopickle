package boopickle

import scala.reflect.ClassTag

/**
 * Encodes a class belonging to a type hierarchy. Type is identified by the index in the `picklers` sequence, so care
 * must be taken to ensure picklers are added in the same order.
 */
case class CompositePickler[A <: AnyRef](var picklers:Vector[(String, Pickler[_])] = Vector()) extends Pickler[A] {
  import Constants._
  override def pickle(obj: A)(implicit state: PickleState): Unit = {
    if(obj == null) {
      state.enc.writeInt(CompositeNull)
    } else {
      val name = obj.getClass.getName
      val idx = picklers.indexWhere(_._1 == name)
      if(idx == -1)
        throw new IllegalArgumentException(s"CompositePickler doesn't know class '$name'. Known classes: ${picklers.map(_._1).mkString(", ")}")
      state.enc.writeInt(idx + 1)
      picklers(idx)._2.asInstanceOf[Pickler[A]].pickle(obj)
    }
  }

  def addConcreteType[B <: A](implicit p: Pickler[B], tag: ClassTag[B]): CompositePickler[A] = {
    picklers = this.picklers :+ (tag.runtimeClass.getName -> p)
    this
  }
}

/**
 * Decodes a class belonging to a type hierarchy. Type is identified by the index in the `unpicklers` sequence, so care
 * must be taken to ensure unpicklers are added in the same order.
 */
case class CompositeUnpickler[A <: AnyRef](var unpicklers:Vector[(String, Unpickler[_])] = Vector()) extends Unpickler[A] {
  override def unpickle(implicit state: UnpickleState): A = {
    val idx = state.dec.readInt
    if(idx == 0)
      null.asInstanceOf[A]
    else {
      if(idx < 0 || idx > unpicklers.length)
        throw new IllegalStateException(s"Index $idx is not defined in this CompositeUnpickler")
      unpicklers(idx-1)._2.asInstanceOf[Unpickler[A]].unpickle
    }
  }

  def addConcreteType[B <: A](implicit p: Unpickler[B], tag: ClassTag[B]): CompositeUnpickler[A] = {
    unpicklers = this.unpicklers :+ (tag.runtimeClass.getName -> p)
    this
  }
}

object CompositePickler {
  def apply[A <: AnyRef] = new PicklerPair[A]()
}

/** Helper for registration of Pickler[B]/Unpickler[B] pairs via `withSubtype[B]`*/
case class PicklerPair[A <: AnyRef](pickler: CompositePickler[A] = new CompositePickler[A](),
                                    unpickler: CompositeUnpickler[A] = new CompositeUnpickler[A]()) {

  def addConcreteType[B <: A](implicit p: Pickler[B], u: Unpickler[B], tag: ClassTag[B]) = {
    pickler.addConcreteType[B]
    unpickler.addConcreteType[B]
    this
  }
}