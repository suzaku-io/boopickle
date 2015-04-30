package boopickle

import java.nio.{CharBuffer, ByteBuffer}
import java.nio.charset.{CharsetEncoder, StandardCharsets}

object StringCodec {
  def decodeUTF8(len:Int, buf:ByteBuffer):String = {
    if(buf.hasArray) {
      val s = new String(buf.array, buf.position, len, "UTF-8")
      buf.position(buf.position + len)
      s
    } else {
      val strBytes = new Array[Byte](len)
      buf.get(strBytes)
      new String(strBytes, "UTF-8")
    }
  }

  def encodeUTF8(str:String):ByteBuffer = {
    ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8))
  }
}
