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
class TextDecoder(utfLabel: js.UndefOr[String] = js.undefined) extends js.Object {
  def decode(data: ArrayBufferView): String = js.native
}

/**
  * Facade for native JS engine provided TextEncoder
  */
@js.native
class TextEncoder(utfLabel: js.UndefOr[String] = js.undefined) extends js.Object {
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

  private lazy val utf16decoder: (Uint16Array) => String = {
    /*
      try {
          // do not use native TextDecoder as it's slow
          val td = new TextDecoder("utf-16none")
          (data: Uint16Array) => td.decode(data)
        } catch {
          case e: Throwable =>
    */
    (data: Uint16Array) =>
      js.Dynamic.global.String.fromCharCode.applyDynamic("apply")(null, data).asInstanceOf[String]
  }

  private lazy val utf16encoder: (String) => Int8Array = {
    /*
      try {
          // do not use native TextEncoder as it's slow
          val te = new TextEncoder("utf-16none")
          (str: String) => te.encode(str)
        } catch {
          case e: Throwable =>
          }
    */
    (str: String) => {
      val ta = new Uint16Array(str.length)
      var i = 0
      while (i < str.length) {
        ta(i) = str.charAt(i).toInt
        i += 1
      }
      new Int8Array(ta.buffer)
    }
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
      val ta = new Uint16Array(buf.typedArray().buffer, buf.position + buf.typedArray().byteOffset, len / 2)
      buf.position(buf.position + len)
      utf16decoder(ta)
      //new String(ta.toArray) // alt implementation
    } else {
      val a = new Array[Byte](len)
      buf.get(a)
      new String(a, StandardCharsets.UTF_16LE)
    }
  }

  override def encodeUTF16(str: String): ByteBuffer = {
    TypedArrayBuffer.wrap(utf16encoder(str))
  }


  override def decodeFast(len: Int, buf: ByteBuffer): String = decodeUTF16(len, buf)

  override def encodeFast(str: String): ByteBuffer = encodeUTF16(str)
}
