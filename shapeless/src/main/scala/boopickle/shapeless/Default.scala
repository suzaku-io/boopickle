package boopickle.shapeless

import boopickle._

object Default extends Base with BasicImplicitPicklers with ShapelessPicklers {

  def generatePickler[T](implicit pickler: Pickler[T]): Pickler[T] = pickler
}
