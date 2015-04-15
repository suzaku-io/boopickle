package boopickle

import java.nio.ByteBuffer

import scala.collection.mutable
import scala.language.higherKinds
import scala.reflect.ClassTag

object Unpickle {
  def apply[A](implicit u: Unpickler[A]) = UnpickledCurry(u)
}

case class UnpickledCurry[A](u: Unpickler[A]) {
  def fromBytes(bytes: ByteBuffer): A = {
    u.unpickle(new UnpickleState(new Decoder(bytes)))
  }
}

trait Unpickler[A] {
  def unpickle(implicit state: UnpickleState): A
}

object Unpickler {

  import Constants._

  implicit object BooleanUnpickler extends Unpickler[Boolean] {
    override def unpickle(implicit state: UnpickleState): Boolean = {
      if (state.dec.readByte == 1)
        true
      else
        false
    }
  }

  implicit object ByteUnpickler extends Unpickler[Byte] {
    override def unpickle(implicit state: UnpickleState): Byte = {
      state.dec.readByte
    }
  }

  implicit object ShortUnpickler extends Unpickler[Short] {
    override def unpickle(implicit state: UnpickleState): Short = {
      state.dec.readInt.toShort
    }
  }

  implicit object IntUnpickler extends Unpickler[Int] {
    override def unpickle(implicit state: UnpickleState): Int = {
      state.dec.readInt
    }
  }

  implicit object LongUnpickler extends Unpickler[Long] {
    override def unpickle(implicit state: UnpickleState): Long = {
      state.dec.readLong
    }
  }

  implicit object FloatUnpickler extends Unpickler[Float] {
    override def unpickle(implicit state: UnpickleState): Float = {
      state.dec.readFloat
    }
  }

  implicit object DoubleUnpickler extends Unpickler[Double] {
    override def unpickle(implicit state: UnpickleState): Double = {
      state.dec.readDouble
    }
  }

  implicit object StringUnpickler extends Unpickler[String] {
    override def unpickle(implicit state: UnpickleState): String = {
      state.dec.readLength match {
        case Left(code) =>
          throw new IllegalArgumentException("Unknown string length coding")
        case Right(0) => ""
        case Right(idx) if idx < 0 =>
          state.immutableFor[String](-idx)
        case Right(len) =>
          val s = state.dec.readString(len)
          if (len < MaxRefStringLen)
            state.addImmutableRef(s)
          s
      }
    }
  }

  implicit def OptionUnpickler[T: Unpickler](implicit u: Unpickler[T]): Unpickler[Option[T]] = new Unpickler[Option[T]] {
    override def unpickle(implicit state: UnpickleState): Option[T] = {
      state.dec.readInt match {
        case 0 =>
          val o = Some(u.unpickle)
          state.addIdentityRef(o)
          o
        case idx if idx < 0 =>
          state.identityFor(-idx)
      }
    }
  }

  import collection.generic.CanBuildFrom

  /**
   * Unpickler for all iterables that have a builder
   * @param cbf Builder for this iterable type
   * @param u Unpickler for the objects in the iterable
   * @tparam T Type of the data objects
   * @tparam V Type of the iterable
   * @return
   */
  implicit def SeqishUnpickler[T: Unpickler, V[_] <: Iterable[_]]
  (implicit cbf: CanBuildFrom[Nothing, T, V[T]], u: Unpickler[T]): Unpickler[V[T]] = new Unpickler[V[T]] {
    override def unpickle(implicit state: UnpickleState): V[T] = {
      state.dec.readLength match {
        case Left(code) =>
          throw new IllegalArgumentException("Unknown sequence length coding")
        case Right(0) =>
          // empty sequence
          cbf().result()
        case Right(idx) if idx < 0 =>
          state.identityFor[V[T]](-idx)
        case Right(len) =>
          val b = cbf()
          for (i <- 0 until len) {
            b += u.unpickle
          }
          val res = b.result()
          state.addIdentityRef(res)
          res
      }
    }
  }

  implicit def ArrayUnpickler[T: Unpickler : ClassTag](implicit u: Unpickler[T]): Unpickler[Array[T]] = new Unpickler[Array[T]] {
    override def unpickle(implicit state: UnpickleState): Array[T] = {
      state.dec.readLength match {
        case Left(code) =>
          throw new IllegalArgumentException("Unknown sequence length coding")
        case Right(0) =>
          // empty Array
          Array.empty[T]
        case Right(idx) if idx < 0 =>
          state.identityFor[Array[T]](-idx)
        case Right(len) =>
          val a = new Array[T](len)
          for (i <- 0 until len) {
            a(i) = u.unpickle
          }
          state.addIdentityRef(a)
          a
      }
    }
  }
  implicit def MapUnpickler[T: Unpickler, S: Unpickler, V[_, _] <: scala.collection.Map[T, S]]
  (implicit cbf: CanBuildFrom[Nothing, (T, S), V[T, S]], ut: Unpickler[T], us: Unpickler[S]): Unpickler[V[T, S]] = new Unpickler[V[T, S]] {
    override def unpickle(implicit state: UnpickleState): V[T, S] = {
      state.dec.readLength match {
        case Left(code) =>
          throw new IllegalArgumentException("Unknown sequence length coding")
        case Right(0) =>
          // empty map
          cbf().result()
        case Right(idx) if idx < 0 =>
          state.identityFor[V[T, S]](-idx)
        case Right(len) =>
          val b = cbf()
          for (i <- 0 until len) {
            b += ut.unpickle -> us.unpickle
          }
          val res = b.result()
          state.addIdentityRef(res)
          res
      }
    }
  }
}

final class UnpickleState(val dec: Decoder) {
  /**
   * Object reference for pickled immutable objects
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

  private[boopickle] def immutableFor[A <: AnyRef](ref: Int): A = {
    assert(ref > 0)
    immutableRefs(ref).asInstanceOf[A]
  }

  private[boopickle] def addImmutableRef(obj: AnyRef): Unit = {
    immutableRefs += obj
  }

  /**
   * Object reference for pickled mutable objects (use identity for equality comparison)
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

  private[boopickle] def identityFor[A <: AnyRef](ref: Int): A = {
    assert(ref > 0)
    identityRefs(ref).asInstanceOf[A]
  }

  private[boopickle] def addIdentityRef(obj: AnyRef): Unit = {
    identityRefs += obj
  }
}