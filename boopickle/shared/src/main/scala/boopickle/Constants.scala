package boopickle

private[boopickle] object Constants {
  final val NullRef = -1

  // codes for special strings
  final val StringInt: Byte = 0
  final val StringLong: Byte = 1
  final val StringUUID: Byte = 2
  final val StringUUIDUpper: Byte = 3

  // codes for special Durations
  final val DurationInf: Byte = 1
  final val DurationMinusInf: Byte = 2
  final val DurationUndefined: Byte = 3

  // codes for Either
  final val EitherLeft: Byte = 1
  final val EitherRight: Byte = 2

  // codes for Option
  final val OptionNone: Byte = 0
  final val OptionSome: Byte = 1

  // codes for Composite pickler
  final val CompositeNull: Byte = 0

  // common strings that can be used as references
  val immutableInitData = Seq("null", "true", "false", "0", "1", "-1", "2", "3", "4", "5", "6", "7", "8", "9")

  val identityInitData = Seq(None)
}
