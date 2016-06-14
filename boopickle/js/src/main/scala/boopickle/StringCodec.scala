package boopickle

import java.nio.{ByteBuffer, ShortBuffer}
import java.nio.charset.StandardCharsets

import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray._

/**
  * Facade for native JS engine provided TextDecoder
  */
@js.native
class TextDecoder extends js.Object {
  def decode(data: Int8Array): String = js.native
}

/**
  * Facade for native JS engine provided TextEncoder
  */
@js.native
class TextEncoder extends js.Object {
  def encode(str: String): Uint8Array = js.native
}

object StringCodec extends StringCodecFuncs {
  private lazy val utf8decoder: (Int8Array) => String = {
    val td = new TextDecoder
    // use native TextDecoder
    (data: Int8Array) => td.decode(data)
  }

  private lazy val utf8encoder: (String) => Int8Array = {
    val te = new TextEncoder
    // use native TextEncoder
    (str: String) => new Int8Array(te.encode(str))
  }

  override def decodeUTF8(len: Int, buf: ByteBuffer): String = {
    if (buf.isDirect && !js.isUndefined(js.Dynamic.global.TextDecoder)) {
      // get the underlying Int8Array
      val ta = buf.typedArray()
      val s = utf8decoder(ta.subarray(buf.position, buf.position + len))
      buf.position(buf.position + len)
      s
    } else {
      val a = new Array[Byte](len)
      buf.get(a)
      new String(a, StandardCharsets.UTF_8)
    }
  }

  override def encodeUTF8(str: String): ByteBuffer = {
    if (js.isUndefined(js.Dynamic.global.TextEncoder)) {
      ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8))
    } else {
      TypedArrayBuffer.wrap(utf8encoder(str))
    }
  }

  override def decodeUTF16(len: Int, buf: ByteBuffer): String = {
    if (buf.isDirect) {
      val ta = new Uint16Array(buf.typedArray().buffer, buf.position + buf.typedArray().byteOffset, len/2)
      buf.position(buf.position + len)
      js.Dynamic.global.String.fromCharCode.applyDynamic("apply")(null, ta).asInstanceOf[String]
      //new String(ta.toArray) // alt implementation
    } else {
      val a = new Array[Byte](len)
      buf.get(a)
      new String(a, StandardCharsets.UTF_16LE)
    }
  }

  override def encodeUTF16(str: String): ByteBuffer = {
    val ta = new Uint16Array(str.length)
    var i = 0
    while (i < str.length) {
      ta(i) = str.charAt(i).toInt
      i += 1
    }
    TypedArrayBuffer.wrap(new Int8Array(ta.buffer))
  }

  override def decodeFast(len: Int, buf: ByteBuffer): String = decodeUTF16(len, buf)

  override def encodeFast(str: String): ByteBuffer = encodeUTF16(str)
}
