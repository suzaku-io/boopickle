package boopickle

import java.nio.ByteBuffer
import java.util.UUID

import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.higherKinds
import scala.reflect.ClassTag

trait Pickler[A] {
  def pickle(obj: A)(implicit state: PickleState)
  def unpickle(implicit state: UnpickleState): A

  def xmap[B](ab: A => B)(ba: B => A): Pickler[B] = {
    val self = this
    new Pickler[B] {
      override def unpickle(implicit state: UnpickleState): B = {
        ab(self.unpickle(state))
      }
      override def pickle(obj: B)(implicit state: PickleState): Unit = {
        self.pickle(ba(obj))
      }
    }
  }
}

/**
 * A Pickler that always returns a constant value.
 *
 * Stores nothing in the pickled output.
 */
final case class ConstPickler[A](a: A) extends Pickler[A] {
  @inline override def pickle(x: A)(implicit s: PickleState) = ()
  @inline override def unpickle(implicit s: UnpickleState) = a
}

trait PicklerHelper {
  protected type P[A] = Pickler[A]

  /**
   * Helper function to write pickled types
   */
  protected def write[A](value: A)(implicit state: PickleState, p: P[A]): Unit = p.pickle(value)(state)

  /**
   * Helper function to unpickle a type
   */
  protected def read[A](implicit state: UnpickleState, u: P[A]): A = u.unpickle
}

object BasicPicklers extends PicklerHelper {

  import Constants._

  val UnitPickler = ConstPickler(())

  object BooleanPickler extends P[Boolean] {
    @inline override def pickle(value: Boolean)(implicit state: PickleState): Unit = state.enc.writeByte(if (value) 1 else 0)
    @inline override def unpickle(implicit state: UnpickleState): Boolean = {
      state.dec.readByte match {
        case 1 => true
        case 0 => false
        case x => throw new IllegalArgumentException(s"Invalid value $x for Boolean")
      }
    }
  }

  object BytePickler extends P[Byte] {
    @inline override def pickle(value: Byte)(implicit state: PickleState): Unit = state.enc.writeByte(value)
    @inline override def unpickle(implicit state: UnpickleState): Byte = state.dec.readByte
  }

  object ShortPickler extends P[Short] {
    @inline override def pickle(value: Short)(implicit state: PickleState): Unit = state.enc.writeInt(value)
    @inline override def unpickle(implicit state: UnpickleState): Short = state.dec.readInt.toShort
  }

  object CharPickler extends P[Char] {
    @inline override def pickle(value: Char)(implicit state: PickleState): Unit = state.enc.writeChar(value)
    @inline override def unpickle(implicit state: UnpickleState): Char = state.dec.readChar
  }

  object IntPickler extends P[Int] {
    @inline override def pickle(value: Int)(implicit state: PickleState): Unit = state.enc.writeInt(value)
    @inline override def unpickle(implicit state: UnpickleState): Int = state.dec.readInt
  }

  object LongPickler extends P[Long] {
    @inline override def pickle(value: Long)(implicit state: PickleState): Unit = state.enc.writeLong(value)
    @inline override def unpickle(implicit state: UnpickleState): Long = state.dec.readLong
  }

  object FloatPickler extends P[Float] {
    @inline override def pickle(value: Float)(implicit state: PickleState): Unit = state.enc.writeFloat(value)
    @inline override def unpickle(implicit state: UnpickleState): Float = state.dec.readFloat
  }

  object DoublePickler extends P[Double] {
    @inline override def pickle(value: Double)(implicit state: PickleState): Unit = state.enc.writeDouble(value)
    @inline override def unpickle(implicit state: UnpickleState): Double = state.dec.readDouble
  }

  object ByteBufferPickler extends P[ByteBuffer] {
    @inline override def pickle(bb: ByteBuffer)(implicit state: PickleState): Unit = state.enc.writeByteBuffer(bb)
    @inline override def unpickle(implicit state: UnpickleState): ByteBuffer = state.dec.readByteBuffer
  }

  object BigIntPickler extends P[BigInt] {
    implicit def bp = BytePickler

    @inline override def pickle(value: BigInt)(implicit state: PickleState): Unit = {
      ArrayPickler.pickle(value.toByteArray)
    }
    @inline override def unpickle(implicit state: UnpickleState): BigInt = {
      BigInt(ArrayPickler.unpickle)
    }
  }

  object BigDecimalPickler extends P[BigDecimal] {
    implicit def bp = BytePickler

    @inline override def pickle(value: BigDecimal)(implicit state: PickleState): Unit = {
      state.enc.writeInt(value.scale)
      ArrayPickler.pickle(value.underlying().unscaledValue.toByteArray)
    }
    @inline override def unpickle(implicit state: UnpickleState): BigDecimal = {
      val scale = state.dec.readInt
      val arr = ArrayPickler.unpickle
      BigDecimal(BigInt(arr), scale)
    }
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
    override def unpickle(implicit state: UnpickleState): String = {
      state.dec.readIntCode match {
        // handle special codings
        case Left(code) if code == specialCode(StringInt) =>
          val i = state.dec.readInt
          String.valueOf(i)
        case Left(code) if code == specialCode(StringLong) =>
          val l = state.dec.readRawLong
          String.valueOf(l)
        case Left(code) if code == specialCode(StringUUID) || code == specialCode(StringUUIDUpper) =>
          val uuid = new UUID(state.dec.readRawLong, state.dec.readRawLong)
          val s = uuid.toString
          if (code == specialCode(StringUUIDUpper))
            s.toUpperCase
          else
            s
        case Left(_) =>
          throw new IllegalArgumentException("Unknown string coding")
        case Right(0) => ""
        case Right(idx) if idx < 0 =>
          state.immutableFor[String](-idx)
        case Right(len) =>
          val s = state.dec.readString(len)
          state.addImmutableRef(s)
          s
      }
    }
  }

  object UUIDPickler extends P[UUID] {
    override def pickle(s: UUID)(implicit state: PickleState): Unit = {
      state.enc.writeRawLong(s.getMostSignificantBits)
      state.enc.writeRawLong(s.getLeastSignificantBits)
    }
    @inline override def unpickle(implicit state: UnpickleState): UUID = {
      new UUID(state.dec.readRawLong, state.dec.readRawLong)
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
    @inline override def unpickle(implicit state: UnpickleState): Duration = {
      state.dec.readLongCode match {
        case Left(c) if c == specialCode(DurationInf) =>
          Duration.Inf
        case Left(c) if c == specialCode(DurationMinusInf) =>
          Duration.MinusInf
        case Left(c) if c == specialCode(DurationUndefined) =>
          Duration.Undefined
        case Right(value) =>
          Duration.fromNanos(value)
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
    override def unpickle(implicit state: UnpickleState): Option[T] = {
      state.dec.readIntCode match {
        case Right(OptionSome) =>
          val o = Some(read[T])
          state.addIdentityRef(o)
          o
        case Right(idx) if idx < 0 =>
          state.identityFor[Option[T]](-idx)
        case _ =>
          throw new IllegalArgumentException("Invalid coding for Option type")
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
    override def unpickle(implicit state: UnpickleState): Either[T, S] = {
      state.dec.readIntCode match {
        case Right(EitherLeft) =>
          Left(read[T])
        case Right(EitherRight) =>
          Right(read[S])
        case Right(idx) if idx < 0 =>
          state.identityFor[Either[T, S]](-idx)
        case _ =>
          throw new IllegalArgumentException("Invalid coding for Either type")
      }
    }
  }

  def LeftPickler[T: P, S: P]: P[Left[T, S]] = EitherPickler[T, S].asInstanceOf[P[Left[T, S]]]

  def RightPickler[T: P, S: P]: P[Right[T, S]] = EitherPickler[T, S].asInstanceOf[P[Right[T, S]]]

  import collection.generic.CanBuildFrom
  /**
   * This pickler works on all collections that derive from Iterable (Vector, Set, List, etc)
   * @tparam T type of the values
   * @tparam V type of the collection
   * @return
   */
  def IterablePickler[T: P, V[_] <: Iterable[_]](implicit cbf: CanBuildFrom[Nothing, T, V[T]]): P[V[T]] = new P[V[T]] {
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
    override def unpickle(implicit state: UnpickleState): V[T] = {
      state.dec.readIntCode match {
        case Left(code) =>
          throw new IllegalArgumentException("Unknown sequence length coding")
        case Right(0) =>
          // empty sequence
          val res = cbf().result()
          state.addIdentityRef(res)
          res
        case Right(idx) if idx < 0 =>
          state.identityFor[V[T]](-idx)
        case Right(len) =>
          val b = cbf()
          for (i <- 0 until len) {
            b += read[T]
          }
          val res = b.result()
          state.addIdentityRef(res)
          res
      }
    }
  }

  /**
   * Specific pickler for Arrays
   * @tparam T Type of values
   * @return
   */
  def ArrayPickler[T: P : ClassTag]: P[Array[T]] = new P[Array[T]] {
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
    override def unpickle(implicit state: UnpickleState): Array[T] = {
      state.dec.readIntCode match {
        case Left(code) =>
          throw new IllegalArgumentException("Unknown sequence length coding")
        case Right(0) =>
          // empty Array
          val a = Array.empty[T]
          state.addIdentityRef(a)
          a
        case Right(idx) if idx < 0 =>
          state.identityFor[Array[T]](-idx)
        case Right(len) =>
          val a = new Array[T](len)
          for (i <- 0 until len) {
            a(i) = read[T]
          }
          state.addIdentityRef(a)
          a
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
  def MapPickler[T: P, S: P, V[_, _] <: scala.collection.Map[_, _]]
  (implicit cbf: CanBuildFrom[Nothing, (T, S), V[T, S]]): P[V[T, S]] = new P[V[T, S]] {
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
    override def unpickle(implicit state: UnpickleState): V[T, S] = {
      state.dec.readIntCode match {
        case Left(code) =>
          throw new IllegalArgumentException("Unknown sequence length coding")
        case Right(0) =>
          // empty map
          val res = cbf().result()
          state.addIdentityRef(res)
          res
        case Right(idx) if idx < 0 =>
          state.identityFor[V[T, S]](-idx)
        case Right(len) =>
          val b = cbf()
          for (i <- 0 until len) {
            b += read[T] -> read[S]
          }
          val res = b.result()
          state.addIdentityRef(res)
          res
      }
    }
  }
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
    identityRefs += ((Identity(obj), identityIdx))
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

final class UnpickleState(val dec: Decoder) {
  /**
   * Object reference for pickled immutable objects. Currently only for strings.
   *
   * Index 0 is not used
   * Index 1 = null
   * Index 2-n, references to pickled immutable objects
   */
  private[this] val immutableRefs = new mutable.ArrayBuffer[AnyRef](16)

  // initialize with basic data
  addImmutableRef(null)
  addImmutableRef(null)
  Constants.immutableInitData.foreach(addImmutableRef)

  @inline def immutableFor[A <: AnyRef](ref: Int): A = {
    assert(ref > 0)
    immutableRefs(ref).asInstanceOf[A]
  }

  @inline def addImmutableRef(obj: AnyRef): Unit = {
    immutableRefs += obj
  }

  /**
   * Object reference for pickled objects (use identity for equality comparison)
   *
   * Index 0 is not used
   * Index 1 = null
   * Index 2-n, references to pickled objects
   */
  private[this] val identityRefs = new mutable.ArrayBuffer[AnyRef](16)

  // initialize with basic data
  addIdentityRef(null)
  addIdentityRef(null)
  Constants.identityInitData.foreach(addIdentityRef)

  @inline def identityFor[A <: AnyRef](ref: Int): A = {
    assert(ref > 0)
    identityRefs(ref).asInstanceOf[A]
  }

  @inline def addIdentityRef(obj: AnyRef): Unit = {
    identityRefs += obj
  }

  @inline def unpickle[A](implicit u: Pickler[A]): A = u.unpickle(this)
}

object UnpickleState {
  def apply(bytes: ByteBuffer) = new UnpickleState(new Decoder(bytes))
}
