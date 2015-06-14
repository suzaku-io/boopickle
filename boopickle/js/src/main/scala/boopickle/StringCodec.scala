package boopickle

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray._

/**
 * Facade for native JS engine provided TextDecoder
 */
class TextDecoder extends js.Object {
  def decode(data: Int8Array): String = js.native
}

/**
 * Facade for native JS engine provided TextEncoder
 */
class TextEncoder extends js.Object {
  def encode(str: String): Uint8Array = js.native
}

object StringCodec {
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

  def decodeUTF8(len: Int, buf: ByteBuffer): String = {
    if (buf.isDirect && !js.isUndefined(js.Dynamic.global.TextDecoder)) {
      // get the underlying Int8Array
      val ta = buf.typedArray()
      val s = utf8decoder(ta.subarray(buf.position, buf.position + len))
      buf.position(buf.position + len)
      s
    } else {
      val bb = buf.slice()
      bb.limit(len)
      val s = StandardCharsets.UTF_8.decode(bb).toString
      buf.position(buf.position + len)
      s
    }
  }

  def encodeUTF8(s: String): ByteBuffer = {
    if (js.isUndefined(js.Dynamic.global.TextEncoder)) {
      StandardCharsets.UTF_8.encode(s)
    } else {
      TypedArrayBuffer.wrap(utf8encoder(s))
    }
  }
}
