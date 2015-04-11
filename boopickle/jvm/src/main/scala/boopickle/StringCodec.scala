package boopickle

import java.nio.ByteBuffer

object StringCodec {
  def decodeUTF8(len:Int, buf:ByteBuffer):String = {
    val strBytes = new Array[Byte](len)
    buf.get(strBytes)
    new String(strBytes, "UTF-8")
  }
}
