package boopickle.shapeless

import boopickle._
import _root_.shapeless._

import java.nio.ByteBuffer
import scala.reflect.ClassTag

trait ShapelessPicklers extends TransformPicklers {
  implicit def hconsPickler[H, T <: HList](implicit hp: Lazy[Pickler[H]], tp: Lazy[Pickler[T]]): Pickler[H :: T] = new Pickler[H :: T] {
    override def pickle(list: H :: T)(implicit state: PickleState): Unit = {
      val head :: tail = list
      hp.value.pickle(head)
      tp.value.pickle(tail)
    }

    override def unpickle(implicit state: UnpickleState): H :: T = {
      val head = hp.value.unpickle
      val tail = tp.value.unpickle
      head :: tail
    }
  }

  implicit val hnilPickler: Pickler[HNil] = new Pickler[HNil] {
    override def pickle(list: HNil)(implicit state: PickleState): Unit = ()
    override def unpickle(implicit state: UnpickleState): HNil = HNil
  }

  implicit def genericPickler[A, B](implicit gen: Generic.Aux[A, B], rp: Lazy[Pickler[B]]): Pickler[A] = new Pickler[A] {
    override def pickle(list: A)(implicit state: PickleState): Unit = rp.value.pickle(gen.to(list))
    override def unpickle(implicit state: UnpickleState): A = gen.from(rp.value.unpickle)
  }

  implicit def coproductInlPickler[A, B <: Coproduct](implicit ap: Lazy[Pickler[A]]): Pickler[Inl[A, B]] =  {
    transformPickler[Inl[A, B], A](a => Inl(a))(_.head)(ap.value)
  }

  implicit def coproductInrPickler[A, B <: Coproduct](implicit bp: Lazy[Pickler[B]]): Pickler[Inr[A, B]] =  {
    transformPickler[Inr[A, B], B](a => Inr(a))(_.tail)(bp.value)
  }

  implicit def coproductPickler[A : ClassTag, B <: Coproduct : ClassTag](implicit ap: Lazy[Pickler[A]], bp: Lazy[Pickler[B]]): Pickler[A :+: B] = {
    CompositePickler[A :+: B].addConcreteType[Inl[A, B]].addConcreteType[Inr[A, B]]
  }

  implicit def cnilPickler: Pickler[CNil] = new Pickler[CNil] {
    override def pickle(list: CNil)(implicit state: PickleState): Unit = ()
    override def unpickle(implicit state: UnpickleState): CNil = ??? // CNil should never be reached
  }
}
object ShapelessPicklers extends ShapelessPicklers
