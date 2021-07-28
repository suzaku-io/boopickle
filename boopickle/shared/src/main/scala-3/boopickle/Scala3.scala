package boopickle

import scala.deriving.Mirror

trait MaterializePicklerExplicit {
  inline def generatePickler[A <: AnyRef: Mirror.Of]: Pickler[A] = PicklerMaterializersImpl.derive[A]
}

trait MaterializePicklerFallback {
  implicit inline def generatePickler[A <: AnyRef: Mirror.Of]: Pickler[A] = PicklerMaterializersImpl.derive[A]
}
