package boopickle

import boopickle.Constants.NullRef
import scala.collection.Factory
import scala.language.higherKinds
import scala.collection.immutable.SeqMap

trait XCompatImplicitPicklers1 {
  this: PicklerHelper =>

  implicit def mapPickler[K: P, V: P, M[_, _] <: scala.collection.Map[_, _]](implicit
      f: Factory[(K, V), M[K, V]]
  ): P[M[K, V]] = BasicPicklers.MapPickler[K, V, M]

  implicit def iterablePickler[A: P, F[_] <: Iterable[_]](implicit cbf: Factory[A, F[A]]): P[F[A]] =
    BasicPicklers.IterablePickler[A, F]
}

trait XCompatImplicitPicklers extends XCompatImplicitPicklers1 {
  this: PicklerHelper =>

  implicit def seqMapPickler[K, V](implicit k: P[K], v: P[V]): P[SeqMap[K, V]] =
    BasicPicklers.MapPickler[K, V, SeqMap](k, v, SeqMap.mapFactory)
}

trait XCompatPicklers {
  this: PicklerHelper =>

  /**
    * This pickler works on all collections that derive from Iterable[A] (Vector, Set, List, etc)
    *
    * @tparam A
    *   type of the values
    * @tparam F
    *   type of the collection
    * @return
    */
  def IterablePickler[A: P, F[_] <: Iterable[_]](implicit cbf: Factory[A, F[A]]): P[F[A]] = new P[F[A]] {
    override def pickle(iterable: F[A])(implicit state: PickleState): Unit = {
      if (iterable == null) {
        state.enc.writeInt(NullRef)
      } else {
        // encode length
        state.enc.writeInt(iterable.size)
        // encode contents
        iterable.iterator.asInstanceOf[Iterator[A]].foreach(a => write[A](a))
      }
    }

    override def unpickle(implicit state: UnpickleState): F[A] = {
      state.dec.readInt match {
        case NullRef =>
          null.asInstanceOf[F[A]]
        case 0 =>
          // empty sequence
          val res = cbf.newBuilder.result()
          res
        case len =>
          val b = cbf.newBuilder
          b.sizeHint(len)
          var i = 0
          while (i < len) {
            b += read[A]
            i += 1
          }
          val res = b.result()
          res
      }
    }
  }

  /** Maps require a specific pickler as they have two type parameters. */
  def MapPickler[K: P, V: P, M[_, _] <: scala.collection.Map[_, _]](implicit cbf: Factory[(K, V), M[K, V]]): P[M[K, V]] =
    new P[M[K, V]] {
      override def pickle(map: M[K, V])(implicit state: PickleState): Unit = {
        if (map == null) {
          state.enc.writeInt(NullRef)
        } else {
          // encode length
          state.enc.writeInt(map.size)
          // encode contents as a sequence
          val kPickler = implicitly[P[K]]
          val vPickler = implicitly[P[V]]
          map.asInstanceOf[scala.collection.Map[K, V]].foreach { kv =>
            kPickler.pickle(kv._1)(state)
            vPickler.pickle(kv._2)(state)
          }
        }
      }

      override def unpickle(implicit state: UnpickleState): M[K, V] = {
        state.dec.readInt match {
          case NullRef =>
            null.asInstanceOf[M[K, V]]
          case 0 =>
            // empty map
            val res = cbf.newBuilder.result()
            res
          case idx if idx < 0 =>
            state.identityFor[M[K, V]](-idx)
          case len =>
            val b = cbf.newBuilder
            b.sizeHint(len)
            val kPickler = implicitly[P[K]]
            val vPickler = implicitly[P[V]]
            var i        = 0
            while (i < len) {
              b += kPickler.unpickle(state) -> vPickler.unpickle(state)
              i += 1
            }
            val res = b.result()
            res
        }
      }
    }
}
