package boopickle

private[boopickle] object Constants {
  final val MaxRefStringLen = 256

  /**
   * Code for encoding special values in place of int/length
   * @param code Number from 0 to 15
   * @return Special code byte
   */
  def specialCode(code:Int):Byte = {
    assert(code < 16 && code >= 0)
    (0xF0 | code).toByte
  }

  // codes for special Durations
  final val DurationInf = 1
  final val DurationMinusInf = 2
  final val DurationUndefined = 3

  // codes for Either
  final val EitherLeft = 1
  final val EitherRight = 2

  // codes for Option
  final val OptionSome = 1

  // codes for Composite pickler
  final val CompositeNull = 0

  val immutableInitData = Seq("null", "true", "false", "0", "1", "-1")

  val identityInitData = Seq(None)
}
