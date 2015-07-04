package boopickle

import java.nio.ByteBuffer
import java.util.UUID

import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.higherKinds

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

object BasicPicklers extends PicklerHelper {

  import Constants._

  object NothingPickler extends P[Nothing] {
    override def pickle(b: Nothing)(implicit state: PickleState): Unit = throw new NotImplementedError("Cannot pickle Nothing!")
  }

  object UnitPickler extends P[Unit] {
    @inline override def pickle(b: Unit)(implicit state: PickleState): Unit = ()
  }

  object BooleanPickler extends P[Boolean] {
    @inline override def pickle(value: Boolean)(implicit state: PickleState): Unit = state.enc.writeByte(if (value) 1 else 0)
  }

  object BytePickler extends P[Byte] {
    @inline override def pickle(value: Byte)(implicit state: PickleState): Unit = state.enc.writeByte(value)
  }

  object ShortPickler extends P[Short] {
    @inline override def pickle(value: Short)(implicit state: PickleState): Unit = state.enc.writeInt(value)
  }

  object CharPickler extends P[Char] {
    @inline override def pickle(value: Char)(implicit state: PickleState): Unit = state.enc.writeChar(value)
  }

  object IntPickler extends P[Int] {
    @inline override def pickle(value: Int)(implicit state: PickleState): Unit = state.enc.writeInt(value)
  }

  object LongPickler extends P[Long] {
    @inline override def pickle(value: Long)(implicit state: PickleState): Unit = state.enc.writeLong(value)
  }

  object FloatPickler extends P[Float] {
    @inline override def pickle(value: Float)(implicit state: PickleState): Unit = state.enc.writeFloat(value)
  }

  object DoublePickler extends P[Double] {
    @inline override def pickle(value: Double)(implicit state: PickleState): Unit = state.enc.writeDouble(value)
  }

  object ByteBufferPickler extends P[ByteBuffer] {
    @inline override def pickle(bb: ByteBuffer)(implicit state: PickleState): Unit = state.enc.writeByteBuffer(bb)
  }

  object StringPickler extends P[String] {
    def encodeUUID(str: String, code: Byte)(implicit state: PickleState): Unit = {
      // special coding for lowercase UUID
      val uuid = UUID.fromString(str)
      state.enc.writeByte(code)
      state.enc.writeRawLong(uuid.getMostSignificantBits)
      state.enc.writeRawLong(uuid.getLeastSignificantBits)
    }

    val uuidRE = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r
    val UUIDRE = "[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}".r
    val numRE = "^-?[1-9][0-9]*$".r

    override def pickle(s: String)(implicit state: PickleState): Unit = {
      // check for previously pickled string
      state.immutableRefFor(s) match {
        case Some(idx) =>
          // encode index as negative "length"
          state.enc.writeInt(-idx)
        case None =>
          if (s.length > 1 && s.length < 21 && (s(0).isDigit || s(0) == '-') && numRE.pattern.matcher(s).matches()) {
            // string represents an integer/long
            try {
              val l = java.lang.Long.parseLong(s)
              if (l > Int.MaxValue || l < Int.MinValue) {
                // value bigger than Int
                state.enc.writeByte(specialCode(StringLong))
                state.enc.writeRawLong(l)
              } else {
                // value fits into an Int
                state.enc.writeByte(specialCode(StringInt))
                state.enc.writeInt(l.toInt)
              }
            } catch {
              case e: NumberFormatException =>
                // probably too big for Long even with all the precautions taken above
                state.addImmutableRef(s)
                state.enc.writeString(s)
            }
          } else if (s.length == 36 && uuidRE.pattern.matcher(s).matches()) {
            // lower-case UUID
            encodeUUID(s, specialCode(StringUUID))
          } else if (s.length == 36 && UUIDRE.pattern.matcher(s).matches()) {
            // upper-case UUID
            encodeUUID(s, specialCode(StringUUIDUpper))
          } else {
            // normal string
            if (s.nonEmpty)
              state.addImmutableRef(s)
            state.enc.writeString(s)
          }
      }
    }
  }

  object UUIDPickler extends P[UUID] {
    override def pickle(s: UUID)(implicit state: PickleState): Unit = {
      state.enc.writeRawLong(s.getMostSignificantBits)
      state.enc.writeRawLong(s.getLeastSignificantBits)
    }
  }

  object DurationPickler extends P[Duration] {
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

  def FiniteDurationPickler: P[FiniteDuration] = DurationPickler.asInstanceOf[P[FiniteDuration]]

  def InfiniteDurationPickler: P[Duration.Infinite] = DurationPickler.asInstanceOf[P[Duration.Infinite]]

  def OptionPickler[T: P]: P[Option[T]] = new P[Option[T]] {
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

  def SomePickler[T: P]: P[Some[T]] = OptionPickler[T].asInstanceOf[P[Some[T]]]

  def EitherPickler[T: P, S: P]: P[Either[T, S]] = new P[Either[T, S]] {
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

  def LeftPickler[T: P, S: P]: P[Left[T, S]] = EitherPickler[T, S].asInstanceOf[P[Left[T, S]]]

  def RightPickler[T: P, S: P]: P[Right[T, S]] = EitherPickler[T, S].asInstanceOf[P[Right[T, S]]]

  /**
   * This pickler works on all collections that derive from Iterable (Vector, Set, List, etc)
   * @tparam T type of the values
   * @tparam V type of the collection
   * @return
   */
  def IterablePickler[T: P, V[_] <: Iterable[_]]: P[V[T]] = new P[V[T]] {
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
  def ArrayPickler[T: P]: P[Array[T]] = new P[Array[T]] {
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
  def MapPickler[T: P, S: P, V[_, _] <: scala.collection.Map[_, _]]: P[V[T, S]] = new P[V[T, S]] {
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

  def toPickler[A <: AnyRef](implicit pair: PicklerPair[A]): Pickler[A] = pair.pickler

  def toTransformPickler[A <: AnyRef, B](implicit transform: TransformPickler[A, B]): Pickler[A] = transform.pickler
}

final class PickleState(val enc: Encoder) {

  import PickleState._

  /**
   * Object reference for pickled immutable objects. Currently only for strings.
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

  @inline def immutableRefFor(obj: AnyRef) = immutableRefs.get(obj)

  @inline def addImmutableRef(obj: AnyRef): Unit = {
    immutableRefs += obj -> immutableIdx
    immutableIdx += 1
  }

  /**
   * Object reference for pickled objects (use identity for equality comparison)
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

  @inline def identityRefFor(obj: AnyRef) = identityRefs.get(Identity(obj))

  @inline def addIdentityRef(obj: AnyRef): Unit = {
    identityRefs += Identity(obj) -> identityIdx
    identityIdx += 1
  }

  @inline def pickle[A](value: A)(implicit p: Pickler[A]): PickleState = {
    p.pickle(value)(this)
    this
  }

  def toByteBuffer = enc.asByteBuffer

  def toByteBuffers = enc.asByteBuffers
}

object PickleState {

  /**
   * Provides a default PickleState if none is available implicitly
   * @return
   */
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
