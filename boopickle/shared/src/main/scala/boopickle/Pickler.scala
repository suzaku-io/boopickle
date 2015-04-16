package boopickle

import java.nio.ByteBuffer
import java.util.UUID

import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.experimental.macros
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

trait PicklerHelper {
  type P[A] = Pickler[A]

  /**
   * Helper function to write pickled types
   */
  def write[A](value: A)(implicit state: PickleState, p: P[A]): Unit = p.pickle(value)(state)
}

object Pickler extends TuplePicklers with MaterializePicklerFallback {

  import Constants._

  implicit val NothingPickler = new P[Nothing] {
    override def pickle(b: Nothing)(implicit state: PickleState): Unit = ???
  }

  implicit val UnitPickler = new P[Unit] {
    @inline override def pickle(b: Unit)(implicit state: PickleState): Unit = ()
  }

  implicit object BooleanPickler extends P[Boolean] {
    @inline override def pickle(value: Boolean)(implicit state: PickleState): Unit = state.enc.writeByte(if (value) 1 else 0)
  }

  implicit object BytePickler extends P[Byte] {
    @inline override def pickle(value: Byte)(implicit state: PickleState): Unit = state.enc.writeByte(value)
  }

  implicit object ShortPickler extends P[Short] {
    @inline override def pickle(value: Short)(implicit state: PickleState): Unit = state.enc.writeInt(value)
  }

  implicit object CharPickler extends P[Char] {
    @inline override def pickle(value: Char)(implicit state: PickleState): Unit = state.enc.writeChar(value)
  }

  implicit object IntPickler extends P[Int] {
    @inline override def pickle(value: Int)(implicit state: PickleState): Unit = state.enc.writeInt(value)
  }

  implicit object LongPickler extends P[Long] {
    @inline override def pickle(value: Long)(implicit state: PickleState): Unit = state.enc.writeLong(value)
  }

  implicit object FloatPickler extends P[Float] {
    @inline override def pickle(value: Float)(implicit state: PickleState): Unit = state.enc.writeFloat(value)
  }

  implicit object DoublePickler extends P[Double] {
    @inline override def pickle(value: Double)(implicit state: PickleState): Unit = state.enc.writeDouble(value)
  }

  implicit object StringPickler extends P[String] {
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

  implicit object UUIDPickler extends P[UUID] {
    override def pickle(s: UUID)(implicit state: PickleState): Unit = {
      state.enc.writeRawLong(s.getMostSignificantBits)
      state.enc.writeRawLong(s.getLeastSignificantBits)
    }
  }

  implicit object DurationPickler extends P[Duration] {
    override def pickle(value: Duration)(implicit state: PickleState): Unit = {
      // take care of special Durations
      value match {
        case Duration.Inf =>
          state.enc.writeByte(specialCode(DurationInf))
        case Duration.MinusInf =>
          state.enc.writeByte(specialCode(DurationMinusInf))
        case x if x eq Duration.Undefined =>
          state.enc.writeByte(specialCode(DurationUndefined))
        case x =>
          state.enc.writeLong(x.toNanos)
      }
    }
  }

  @inline implicit def FiniteDurationPickler: P[FiniteDuration] = DurationPickler.asInstanceOf[P[FiniteDuration]]

  @inline implicit def InfiniteDurationPickler: P[Duration.Infinite] = DurationPickler.asInstanceOf[P[Duration.Infinite]]

  implicit def OptionPickler[T: P] = new P[Option[T]] {
    override def pickle(obj: Option[T])(implicit state: PickleState): Unit = {
      obj match {
        case Some(x) =>
          // check if this Option has been pickled already
          state.identityRefFor(obj) match {
            case Some(idx) =>
              // encode index as negative "length"
              state.enc.writeInt(-idx)
            case None =>
              state.enc.writeInt(OptionSome)
              write[T](x)
              state.addIdentityRef(obj)
          }
        case None =>
          // `None` is always encoded as reference
          val idx = state.identityRefFor(obj).get
          state.enc.writeInt(-idx)
      }
    }
  }

  @inline implicit def SomePickler[T](implicit p: P[T]): P[Some[T]] = OptionPickler[T].asInstanceOf[P[Some[T]]]

  implicit def EitherPickler[T: P, S: P]: P[Either[T, S]] = new P[Either[T, S]] {
    override def pickle(obj: Either[T, S])(implicit state: PickleState): Unit = {
      // check if this Either has been pickled already
      state.identityRefFor(obj) match {
        case Some(idx) =>
          // encode index as negative "length"
          state.enc.writeInt(-idx)
        case None =>
          obj match {
            case Left(l) =>
              state.enc.writeInt(EitherLeft)
              write[T](l)
            case Right(r) =>
              state.enc.writeInt(EitherRight)
              write[S](r)
          }
          state.addIdentityRef(obj)
      }
    }
  }

  implicit def LeftPickler[T: P, S: P] = EitherPickler[T, S].asInstanceOf[P[Left[T, S]]]

  implicit def RightPickler[T: P, S: P] = EitherPickler[T, S].asInstanceOf[P[Right[T, S]]]

  /**
   * This pickler works on all collections that derive from Iterable (Vector, Set, List, etc)
   * @tparam T type of the values
   * @tparam V type of the collection
   * @return
   */
  implicit def iterablePickler[T: P, V[_] <: Iterable[_]] = new P[V[T]] {
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
          iterable.iterator.asInstanceOf[Iterator[T]].foreach(a => write[T](a))
          state.addIdentityRef(iterable)
      }
    }
  }

  /**
   * Specific pickler for Arrays
   * @tparam T Type of values
   * @return
   */
  implicit def arrayPickler[T: P] = new P[Array[T]] {
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
          array.foreach(a => write[T](a))
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
  implicit def mapPickler[T: P, S: P, V[_, _] <: scala.collection.Map[_, _]] = new P[V[T, S]] {
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
          map.iterator.asInstanceOf[Iterator[(T, S)]].foreach { a => write[T](a._1); write[S](a._2) }
          state.addIdentityRef(map)
      }
    }
  }

  implicit def toPickler[A <: AnyRef](implicit pair: PicklerPair[A]): Pickler[A] = pair.pickler
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

trait MaterializePicklerFallback {
  implicit def materializePickler[T]: Pickler[T] = macro PicklerMaterializersImpl.materializePickler[T]
}