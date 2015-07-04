package boopickle

import java.nio.{ByteBuffer, ByteOrder}

import scala.collection.generic.CanBuildFrom
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.Try

trait BasicImplicitPicklers extends PicklerHelper {
  implicit val NothingPickler = BasicPicklers.NothingPickler
  implicit val UnitPickler = BasicPicklers.UnitPickler
  implicit val BooleanPickler = BasicPicklers.BooleanPickler
  implicit val BytePickler = BasicPicklers.BytePickler
  implicit val ShortPickler = BasicPicklers.ShortPickler
  implicit val CharPickler = BasicPicklers.CharPickler
  implicit val IntPickler = BasicPicklers.IntPickler
  implicit val LongPickler = BasicPicklers.LongPickler
  implicit val FloatPickler = BasicPicklers.FloatPickler
  implicit val DoublePickler = BasicPicklers.DoublePickler
  implicit val ByteBufferPickler = BasicPicklers.ByteBufferPickler
  implicit val StringPickler = BasicPicklers.StringPickler
  implicit val UUIDPickler = BasicPicklers.UUIDPickler
  implicit val DurationPickler = BasicPicklers.DurationPickler
  implicit val FiniteDurationPickler = BasicPicklers.FiniteDurationPickler
  implicit val InfiniteDurationPickler = BasicPicklers.InfiniteDurationPickler

  implicit def OptionPickler[T: P] = BasicPicklers.OptionPickler[T]
  implicit def SomePickler[T: P] = BasicPicklers.SomePickler[T]
  implicit def EitherPickler[T: P, S: P] = BasicPicklers.EitherPickler[T, S]
  implicit def LeftPickler[T: P, S: P] = BasicPicklers.LeftPickler[T, S]
  implicit def RightPickler[T: P, S: P] = BasicPicklers.RightPickler[T, S]
  implicit def IterablePickler[T: P, V[_] <: Iterable[_]]
    (implicit cbf: CanBuildFrom[Nothing, T, V[T]]): P[V[T]] = BasicPicklers.IterablePickler[T, V]
  implicit def ArrayPickler[T: P : ClassTag]: P[Array[T]] = BasicPicklers.ArrayPickler[T]
  implicit def MapPickler[T: P, S: P, V[_, _] <: scala.collection.Map[_, _]]
    (implicit cbf: CanBuildFrom[Nothing, (T, S), V[T, S]]): P[V[T, S]] = BasicPicklers.MapPickler[T, S, V]

}

trait TransformPicklers {
  implicit def toTransformPickler[A <: AnyRef, B](implicit transform: TransformPickler[A, B]): Pickler[A] = BasicPicklers.toTransformPickler[A, B]
}

trait MaterializePicklerFallback {
  implicit def generatePickler[T]: Pickler[T] = macro PicklerMaterializersImpl.materializePickler[T]
}

object PickleImpl {
  def apply[A](value: A)(implicit state: PickleState, p: Pickler[A]): PickleState = {
    p.pickle(value)(state)
    state
  }

  def intoBytes[A](value: A)(implicit state: PickleState, p: Pickler[A]): ByteBuffer = {
    apply(value).toByteBuffer
  }

  def intoByteBuffers[A](value: A)(implicit state: PickleState, p: Pickler[A]): Iterable[ByteBuffer] = {
    apply(value).toByteBuffers
  }
}

object UnpickleImpl {
  def apply[A](implicit u: Pickler[A]) = UnpickledCurry(u)

  case class UnpickledCurry[A](u: Pickler[A]) {
    def apply(implicit state: UnpickleState): A = u.unpickle(state)

    def fromBytes(bytes: ByteBuffer): A = {
      // keep original byte order
      val origByteOrder = bytes.order()
      // but decode as little-endian
      val result = u.unpickle(new UnpickleState(new Decoder(bytes.order(ByteOrder.LITTLE_ENDIAN))))
      bytes.order(origByteOrder)
      result
    }

    def tryFromBytes(bytes: ByteBuffer): Try[A] = Try(fromBytes(bytes))

    def fromState(state: UnpickleState): A = u.unpickle(state)
  }

}

trait Base {
  type Pickler[A] = _root_.boopickle.Pickler[A]
  val Pickle = _root_.boopickle.PickleImpl
  type PickleState = _root_.boopickle.PickleState

  val Unpickle = _root_.boopickle.UnpickleImpl

  type UnpickleState = _root_.boopickle.UnpickleState
}

/**
 * Provides basic implicit picklers including macro support for case classes
 */
object Default extends Base with
BasicImplicitPicklers with
TransformPicklers with
TuplePicklers with
MaterializePicklerFallback

/**
 * Provides basic implicit picklers without macro support for case classes
 */
object DefaultBasic extends Base with
BasicImplicitPicklers with
TransformPicklers with
TuplePicklers