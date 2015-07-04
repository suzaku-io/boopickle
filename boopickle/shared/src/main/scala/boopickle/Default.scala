package boopickle

import java.nio.{ByteBuffer, ByteOrder}

import scala.collection.generic.CanBuildFrom
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.Try

trait BasicImplicitPicklers extends PicklerHelper with UnpicklerHelper {
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
  implicit def IterablePickler[T: P, V[_] <: Iterable[_]]: P[V[T]] = BasicPicklers.IterablePickler[T, V]
  implicit def ArrayPickler[T: P]: P[Array[T]] = BasicPicklers.ArrayPickler[T]
  implicit def MapPickler[T: P, S: P, V[_, _] <: scala.collection.Map[_, _]]: P[V[T, S]] = BasicPicklers.MapPickler[T, S, V]

  implicit val UnitUnpickler = BasicUnpicklers.UnitUnpickler
  implicit val BooleanUnpickler = BasicUnpicklers.BooleanUnpickler
  implicit val ByteUnpickler = BasicUnpicklers.ByteUnpickler
  implicit val ShortUnpickler = BasicUnpicklers.ShortUnpickler
  implicit val CharUnpickler = BasicUnpicklers.CharUnpickler
  implicit val IntUnpickler = BasicUnpicklers.IntUnpickler
  implicit val LongUnpickler = BasicUnpicklers.LongUnpickler
  implicit val FloatUnpickler = BasicUnpicklers.FloatUnpickler
  implicit val DoubleUnpickler = BasicUnpicklers.DoubleUnpickler
  implicit val ByteBufferUnpickler = BasicUnpicklers.ByteBufferUnpickler
  implicit val StringUnpickler = BasicUnpicklers.StringUnpickler
  implicit val UUIDUnpickler = BasicUnpicklers.UUIDUnpickler
  implicit val DurationUnpickler = BasicUnpicklers.DurationUnpickler

  implicit def OptionUnpickler[T: U] = BasicUnpicklers.OptionUnpickler[T]
  implicit def EitherUnpickler[T: U, S: U] = BasicUnpicklers.EitherUnpickler[T, S]
  implicit def IterableUnpickler[T: U, V[_] <: Iterable[_]](implicit cbf: CanBuildFrom[Nothing, T, V[T]]) = BasicUnpicklers.IterableUnpickler[T, V]
  implicit def ArrayUnpickler[T: U : ClassTag] = BasicUnpicklers.ArrayUnpickler[T]
  implicit def MapUnpickler[T: U, S: U, V[_, _] <: scala.collection.Map[_, _]](implicit
                                                                               cbf: CanBuildFrom[Nothing, (T, S), V[T, S]]) = BasicUnpicklers.MapUnpickler[T, S, V]
}

trait PairPicklers {
  implicit def toPickler[A <: AnyRef](implicit pair: PicklerPair[A]): Pickler[A] = BasicPicklers.toPickler[A]
  implicit def toUnpickler[A <: AnyRef](implicit pair: PicklerPair[A]): Unpickler[A] = BasicUnpicklers.toUnpickler[A]
}

trait TransformPicklers {
  implicit def toTransformPickler[A <: AnyRef, B](implicit transform: TransformPickler[A, B]): Pickler[A] = BasicPicklers.toTransformPickler[A, B]
  implicit def toTransformUnpickler[A <: AnyRef, B](implicit transform: TransformPickler[A, B]): Unpickler[A] = BasicUnpicklers.toTransformUnpickler[A, B]
}

trait MaterializePicklerFallback {
  implicit def materializePickler[T]: Pickler[T] = macro PicklerMaterializersImpl.materializePickler[T]
}

trait MaterializeUnpicklerFallback {
  implicit def materializeUnpickler[T]: Unpickler[T] = macro PicklerMaterializersImpl.materializeUnpickler[T]
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
  def apply[A](implicit u: Unpickler[A]) = UnpickledCurry(u)

  case class UnpickledCurry[A](u: Unpickler[A]) {
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

  type Unpickler[A] = _root_.boopickle.Unpickler[A]
  val Unpickle = _root_.boopickle.UnpickleImpl

  type UnpickleState = _root_.boopickle.UnpickleState
}

/**
 * Provides basic implicit picklers including macro support for case classes
 */
object Default extends Base with
BasicImplicitPicklers with
PairPicklers with
TransformPicklers with
TuplePicklers with
TupleUnpicklers with
MaterializePicklerFallback with
MaterializeUnpicklerFallback

/**
 * Provides basic implicit picklers without macro support for case classes
 */
object DefaultBasic extends Base with
BasicImplicitPicklers with
PairPicklers with
TransformPicklers with
TuplePicklers with
TupleUnpicklers
