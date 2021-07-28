package boopickle

import scala.deriving.Mirror

trait PicklerDerived {
  inline def derived[A]: Pickler[A] = PicklerMaterializersImpl.derive[A]
}

trait MaterializePicklerExplicit {
  inline def generatePickler[A]: Pickler[A] = PicklerMaterializersImpl.derive[A]
}

trait MaterializePicklerFallback {
  implicit inline def generatePickler[A]: Pickler[A] = PicklerMaterializersImpl.derive[A]
}
