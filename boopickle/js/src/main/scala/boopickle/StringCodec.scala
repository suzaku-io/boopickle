package boopickle

import java.nio.ByteBuffer

import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray._

class TextDecoder extends js.Object {
  def decode(data: js.Any): String = js.native
}

class TextEncoder extends js.Object {
  def encode(str: String): Int8Array = js.native
}

object StringCodec {
  val utf8decoder = new TextDecoder
  val utf8encoder = new TextEncoder

  def decodeUTF8(len:Int, buf:ByteBuffer):String = {
    // get the underlying Int8Array
    val ta = buf.typedArray()
    val s = utf8decoder.decode(ta.subarray(buf.position, buf.position + len))
    buf.position(buf.position + len)
    s
  }

  def encodeUTF8(s:String):ByteBuffer = {
    TypedArrayBuffer.wrap(utf8encoder.encode(s))
  }
}
