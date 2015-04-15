package boopickle

private object Constants {
  // define some constants
  final val MaxRefStringLen = 64
  val immutableInitData = Seq("null", "true", "false", "0", "1", "-1")
  val identityInitData = Seq(None)
}
