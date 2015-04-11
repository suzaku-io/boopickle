package boopickle

import java.nio.ByteBuffer

import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBufferOps._

class TextDecoder extends js.Object {
  def decode(data: js.Any): String = js.native
}

object StringCodec {
  val utf8decoder = new TextDecoder
  
  def decodeUTF8(len:Int, buf:ByteBuffer):String = {
    // get the underlying Int8Array
    val ta = buf.typedArray()
    utf8decoder.decode(ta.subarray(buf.position, buf.position + len))
  }
}
