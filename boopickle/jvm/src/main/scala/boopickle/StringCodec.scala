package boopickle

import java.nio.{ByteBuffer, CharBuffer}
import java.nio.charset.StandardCharsets

object StringCodec extends StringCodecBase {
  override def decodeUTF8(len: Int, buf: ByteBuffer): String = {
    val a = new Array[Byte](len)
    buf.get(a)
    new String(a, StandardCharsets.UTF_8)
  }

  override def encodeUTF8(str: String): ByteBuffer = {
    ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8))
  }

  override def decodeUTF16(len: Int, buf: ByteBuffer): String = {
    val a = new Array[Byte](len)
    buf.get(a)
    new String(a, StandardCharsets.UTF_16LE)
  }

  override def encodeUTF16(str: String): ByteBuffer = {
    ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_16LE))
  }
/*

  override def decodeFast(len: Int, buf: ByteBuffer): String = decodeUTF8(len, buf)

  override def encodeFast(str: String): ByteBuffer = encodeUTF8(str)
*/
}
