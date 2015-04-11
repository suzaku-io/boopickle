package boopickle

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object StringCodec {
  def decodeUTF8(len:Int, buf:ByteBuffer):String = {
    val strBytes = new Array[Byte](len)
    buf.get(strBytes)
    new String(strBytes, "UTF-8")
  }

  def encodeUTF8(str:String):ByteBuffer = {
    ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8))
  }
}
