package boopickle

trait MaterializePicklerExplicit {
  def generatePickler[T]: Pickler[T] = macro PicklerMaterializersImpl.materializePickler[T]
}

trait MaterializePicklerFallback {
  implicit def generatePickler[T]: Pickler[T] = macro PicklerMaterializersImpl.materializePickler[T]
}
