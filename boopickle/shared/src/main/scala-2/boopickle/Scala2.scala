package boopickle

import scala.language.experimental.macros

trait PicklerDerived {
  // For compatibility with Scala 3
  def derived[T]: Pickler[T] = macro PicklerMaterializersImpl.materializePickler[T]
}

trait MaterializePicklerExplicit {
  def generatePickler[T]: Pickler[T] = macro PicklerMaterializersImpl.materializePickler[T]
}

trait MaterializePicklerFallback {
  implicit def generatePickler[T]: Pickler[T] = macro PicklerMaterializersImpl.materializePickler[T]
}
