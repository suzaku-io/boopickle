package boopickle

import scala.deriving.Mirror

trait MaterializePicklerExplicit {
  inline def generatePickler[A]: Pickler[A] = PicklerMaterializersImpl.derive[A]
}

trait MaterializePicklerFallback {
  implicit inline def generatePickler[A]: Pickler[A] = PicklerMaterializersImpl.derive[A]
}
