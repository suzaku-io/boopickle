package boopickle

import java.nio.ByteBuffer

import scala.collection.mutable
import scala.language.higherKinds

object Pickle {
  def apply[A](value: A)(implicit state: PickleState, p: Pickler[A]): PickleState = {
    p.pickle(value)(state)
    state
  }

  def intoBytes[A](value: A)(implicit state: PickleState, p: Pickler[A]): ByteBuffer = {
    apply(value).bytes
  }
}

trait Pickler[A] {
  def pickle(obj: A)(implicit state: PickleState)
}

object Pickler {

  import Constants._

  implicit val NothingPickler = new Pickler[Nothing] {
    override def pickle(b: Nothing)(implicit state: PickleState): Unit = {
      // do nothing
    }
  }

  implicit object BooleanPickler extends Pickler[Boolean] {
    override def pickle(value: Boolean)(implicit state: PickleState): Unit = {
      state.enc.writeByte(if (value) 1 else 0)
    }
  }

  implicit object BytePickler extends Pickler[Byte] {
    override def pickle(value: Byte)(implicit state: PickleState): Unit = {
      state.enc.writeByte(value)
    }
  }

  implicit object ShortPickler extends Pickler[Short] {
    override def pickle(value: Short)(implicit state: PickleState): Unit = {
      state.enc.writeInt(value)
    }
  }

  implicit object IntPickler extends Pickler[Int] {
    override def pickle(value: Int)(implicit state: PickleState): Unit = {
      state.enc.writeInt(value)
    }
  }

  implicit object LongPickler extends Pickler[Long] {
    override def pickle(value: Long)(implicit state: PickleState): Unit = {
      state.enc.writeLong(value)
    }
  }

  implicit object FloatPickler extends Pickler[Float] {
    override def pickle(value: Float)(implicit state: PickleState): Unit = {
      state.enc.writeFloat(value)
    }
  }

  implicit object DoublePickler extends Pickler[Double] {
    override def pickle(value: Double)(implicit state: PickleState): Unit = {
      state.enc.writeDouble(value)
    }
  }

  implicit object StringPickler extends Pickler[String] {
    override def pickle(s: String)(implicit state: PickleState): Unit = {
      // check for previously pickled string
      state.immutableRefFor(s) match {
        case Some(idx) =>
          // encode index as negative "length"
          state.enc.writeInt(-idx)
        case None =>
          if (s.length < MaxRefStringLen && s.nonEmpty)
            state.addImmutableRef(s)
          state.enc.writeString(s)
      }
    }
  }

  implicit def OptionPickler[T](implicit p: Pickler[T]): Pickler[Option[T]] = new Pickler[Option[T]] {
    override def pickle(obj: Option[T])(implicit state: PickleState): Unit = {
      obj match {
        case Some(x) =>
          state.enc.writeByte(0)
          p.pickle(x)
        case None =>
          val idx = state.identityRefFor(obj).get
          // encode index as negative "length"
          state.enc.writeInt(-idx)
      }
    }
  }

  implicit def SomePickler[T](implicit p: Pickler[T]): Pickler[Some[T]] = OptionPickler[T].asInstanceOf[Pickler[Some[T]]]

  /**
   * This pickler works on all collections that derive from Iterable (Vector, Set, List, etc)
   * @tparam T type of the data objects
   * @tparam V type of the collection
   * @return
   */
  implicit def iterablePickler[T: Pickler, V[_] <: Iterable[_]](implicit p: Pickler[T]): Pickler[V[T]] = new Pickler[V[T]] {
    override def pickle(iterable: V[T])(implicit state: PickleState): Unit = {
      // check if this iterable has been pickled already
      state.identityRefFor(iterable) match {
        case Some(idx) =>
          // encode index as negative "length"
          state.enc.writeInt(-idx)
        case None =>
          // encode length
          state.enc.writeInt(iterable.size)
          // encode contents
          iterable.iterator.asInstanceOf[Iterator[T]].foreach(a => p.pickle(a))
          state.addIdentityRef(iterable)
      }
    }
  }

  implicit def arrayPickler[T: Pickler](implicit p: Pickler[T]): Pickler[Array[T]] = new Pickler[Array[T]] {
    override def pickle(array: Array[T])(implicit state: PickleState): Unit = {
      // check if this iterable has been pickled already
      state.identityRefFor(array) match {
        case Some(idx) =>
          // encode index as negative "length"
          state.enc.writeInt(-idx)
        case None =>
          // encode length
          state.enc.writeInt(array.length)
          // encode contents
          array.foreach(a => p.pickle(a))
          state.addIdentityRef(array)
      }
    }
  }
  /**
   * Maps require a specific pickler as they have two type parameters.
   *
   * @tparam T Type of keys
   * @tparam S Type of values
   * @return
   */
  implicit def mapPickler[T: Pickler, S: Pickler, V[_, _] <: scala.collection.Map[_, _]]: Pickler[V[T, S]] = new Pickler[V[T, S]] {
    override def pickle(map: V[T, S])(implicit state: PickleState): Unit = {
      // check if this map has been pickled already
      state.identityRefFor(map) match {
        case Some(idx) =>
          // encode index as negative "length"
          state.enc.writeInt(-idx)
        case None =>
          // encode length
          state.enc.writeInt(map.size)
          // encode contents as a sequence
          map.iterator.asInstanceOf[Iterator[(T, S)]].foreach(a => Pickle(a))
          state.addIdentityRef(map)
      }
    }
  }

  implicit def Tuple2Pickler[T1: Pickler, T2: Pickler]: Pickler[(T1, T2)] = new Pickler[(T1, T2)] {
    override def pickle(t: (T1, T2))(implicit state: PickleState): Unit = {
      Pickle(t._1)
      Pickle(t._2)
    }
  }
}

final class PickleState(val enc: Encoder) {

  import PickleState._

  /**
   * Object reference for pickled immutable objects
   *
   * Index 0 is not used
   * Index 1 = null
   * Index 2-n, references to pickled immutable objects
   */
  private[this] val immutableRefs = new mutable.AnyRefMap[AnyRef, Int]
  private[this] var immutableIdx = 1

  // initialize with basic data
  addImmutableRef(null)
  Constants.immutableInitData.foreach(addImmutableRef)

  private[boopickle] def immutableRefFor(obj: AnyRef) = immutableRefs.get(obj)

  private[boopickle] def addImmutableRef(obj: AnyRef): Unit = {
    immutableRefs += obj -> immutableIdx
    immutableIdx += 1
  }

  /**
   * Object reference for pickled mutable objects (use identity for equality comparison)
   *
   * Index 0 is not used
   * Index 1 = null
   * Index 2-n, references to pickled objects
   */
  private[this] val identityRefs = new mutable.HashMap[Identity[AnyRef], Int]
  private[this] var identityIdx = 1

  // initialize with basic data
  addIdentityRef(null)
  Constants.identityInitData.foreach(addIdentityRef)

  private[boopickle] def identityRefFor(obj: AnyRef) = identityRefs.get(Identity(obj))

  private[boopickle] def addIdentityRef(obj: AnyRef): Unit = {
    identityRefs += Identity(obj) -> identityIdx
    identityIdx += 1
  }

  def pickle[A](value: A)(implicit p: Pickler[A]): PickleState = {
    p.pickle(value)(this)
    this
  }

  def bytes = enc.encode
}

object PickleState {

  implicit def Default: PickleState = new PickleState(new Encoder)

  private case class Identity[+A <: AnyRef](obj: A) {
    override def equals(that: Any): Boolean = that match {
      case that: Identity[_] => this.obj eq that.obj
      case _ => false
    }

    override def hashCode(): Int =
      System.identityHashCode(obj)
  }

}