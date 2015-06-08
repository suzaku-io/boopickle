package boopickle

import java.nio.{CharBuffer, ByteBuffer}
import java.nio.charset.{CharsetEncoder, StandardCharsets}

object StringCodec {
  def decodeUTF8(len: Int, buf: ByteBuffer): String = {
    val bb = buf.slice()
    bb.limit(len)
    val s = StandardCharsets.UTF_8.decode(bb).toString
    buf.position(buf.position + len)
    s
  }

  def encodeUTF8(str: String): ByteBuffer = {
    ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8))
  }
}
