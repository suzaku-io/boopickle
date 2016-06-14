package boopickle

import java.nio.{ByteBuffer, CharBuffer}
import java.nio.charset.StandardCharsets

object StringCodec extends StringCodecFuncs {
  override def decodeUTF8(len: Int, buf: ByteBuffer): String = {
    val bb = buf.slice()
    bb.limit(len)
    val s = StandardCharsets.UTF_8.decode(bb).toString
    buf.position(buf.position + len)
    s
  }

  override def encodeUTF8(str: String): ByteBuffer = {
    StandardCharsets.UTF_8.encode(str)
  }

  override def decodeUTF16(len: Int, buf: ByteBuffer): String = {
    val bb = buf.slice()
    bb.limit(len)
    val s = StandardCharsets.UTF_16LE.decode(bb).toString
    buf.position(buf.position + len)
    s
  }

  override def encodeUTF16(str: String): ByteBuffer = {
    StandardCharsets.UTF_16LE.encode(str)
  }

  override def decodeFast(len: Int, buf: ByteBuffer): String = decodeUTF8(len, buf)

  override def encodeFast(s: String): ByteBuffer = encodeUTF8(s)
}
