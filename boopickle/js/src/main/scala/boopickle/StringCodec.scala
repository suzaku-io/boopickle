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
  def encode(str: String): Int8Array = js.native
}

object StringCodec {
  private val utf8decoder: (Int8Array) => String = {
    if (js.isUndefined(js.Dynamic.global.TextDecoder)) {
      println("Shimming TextDecoder")
      // emulated functionality
      (data: Int8Array) => new String(data.toArray, StandardCharsets.UTF_8)
    } else {
      println("Native TextDecoder")
      val td = new TextDecoder
      // use native TextDecoder
      (data: Int8Array) => td.decode(data)
    }
  }

  private val utf8encoder: (String) => Int8Array = {
    if (js.isUndefined(js.Dynamic.global.TextEncoder)) {
      println("Shimming TextEncoder")
      // emulated functionality
      (str: String) => new Int8Array(str.getBytes(StandardCharsets.UTF_8).toJSArray)
    } else {
      println("Native TextEncoder")
      val te = new TextEncoder
      // use native TextEncoder
      (str: String) => te.encode(str)
    }
  }

  def decodeUTF8(len: Int, buf: ByteBuffer): String = {
    // get the underlying Int8Array
    val ta = buf.typedArray()
    val s = utf8decoder(ta.subarray(buf.position, buf.position + len))
    buf.position(buf.position + len)
    s
  }

  def encodeUTF8(s: String): ByteBuffer = {
    TypedArrayBuffer.wrap(utf8encoder(s))
  }
}
