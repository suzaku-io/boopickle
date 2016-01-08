package boopickle

import java.nio.{ByteBuffer, ByteOrder}

import scala.collection.generic.CanBuildFrom
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.Try

trait BasicImplicitPicklers extends PicklerHelper {
  implicit val unitPickler = BasicPicklers.UnitPickler
  implicit val booleanPickler = BasicPicklers.BooleanPickler
  implicit val bytePickler = BasicPicklers.BytePickler
  implicit val shortPickler = BasicPicklers.ShortPickler
  implicit val charPickler = BasicPicklers.CharPickler
  implicit val intPickler = BasicPicklers.IntPickler
  implicit val longPickler = BasicPicklers.LongPickler
  implicit val floatPickler = BasicPicklers.FloatPickler
  implicit val doublePickler = BasicPicklers.DoublePickler
  implicit val bigIntPickler = BasicPicklers.BigIntPickler
  implicit val bigDecimalPickler = BasicPicklers.BigDecimalPickler
  implicit val byteBufferPickler = BasicPicklers.ByteBufferPickler
  implicit val stringPickler = BasicPicklers.StringPickler
  implicit val UUIDPickler = BasicPicklers.UUIDPickler
  implicit val durationPickler = BasicPicklers.DurationPickler
  implicit val finiteDurationPickler = BasicPicklers.FiniteDurationPickler
  implicit val infiniteDurationPickler = BasicPicklers.InfiniteDurationPickler

  implicit def optionPickler[T: P] = BasicPicklers.OptionPickler[T]
  implicit def somePickler[T: P] = BasicPicklers.SomePickler[T]
  implicit def eitherPickler[T: P, S: P] = BasicPicklers.EitherPickler[T, S]
  implicit def leftPickler[T: P, S: P] = BasicPicklers.LeftPickler[T, S]
  implicit def rightPickler[T: P, S: P] = BasicPicklers.RightPickler[T, S]
  implicit def iterablePickler[T: P, V[_] <: Iterable[_]](implicit cbf: CanBuildFrom[Nothing, T, V[T]]): P[V[T]] = BasicPicklers.IterablePickler[T, V]
  implicit def arrayPickler[T: P : ClassTag]: P[Array[T]] = BasicPicklers.ArrayPickler[T]
  implicit def mapPickler[T: P, S: P, V[_, _] <: scala.collection.Map[_, _]](implicit
                                                                             cbf: CanBuildFrom[Nothing, (T, S), V[T, S]]): P[V[T, S]] = BasicPicklers.MapPickler[T, S, V]
}

trait TransformPicklers {
  /**
   * Create a transforming pickler that takes an object of type `A` and transforms it into `B`, which is then pickled.
   * Similarly a `B` is unpickled and then transformed back into `A`.
   *
   * This allows for easy creation of picklers for (relatively) simple classes. For example
   * {{{
   *   // transform Date into Long and back
   *   implicit val datePickler = transformPickler[java.util.Date, Long](
   *     _.getTime,
   *     t => new java.util.Date(t))
   * }}}
   *
   * @param transformTo Function that takes `A` and transforms it into `B`
   * @param transformFrom Function that takes `B` and transforms it into `A`
   * @tparam A Type of the original object
   * @tparam B Type for the object used for pickling
   */
  def transformPickler[A, B](transformTo: (A) => B, transformFrom: (B) => A)(implicit p: Pickler[B]) = {
    p.xmap(transformFrom)(transformTo)
  }
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

  def compositePickler[A <: AnyRef] = CompositePickler[A]()

  def exceptionPickler = ExceptionPickler.base
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
TuplePicklers {

  object PicklerGenerator {
    def generatePickler[T]: Pickler[T] = macro PicklerMaterializersImpl.materializePickler[T]
  }

}
