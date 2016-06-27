package boopickle

import java.nio.ByteBuffer

abstract class StringCodecFast {
  def decodeFast(len: Int, buf: ByteBuffer): String = {
    val cp = new Array[Char](len)
    var i = 0
    var dst = 0
    while (i < len) {
      val b = buf.get()
      if((b & 0x80) == 0) {
        cp(dst) = (b & 0x7F).toChar
      } else if((b & 0xC0) == 0x80) {
        val b1 = buf.get()
        i += 1
        cp(dst) = (b & 0x3F | (b1.toShort & 0xFF) << 6).toChar
      } else  {
        val b1 = buf.get()
        val b2 = buf.get()
        i += 2
        cp(dst) = (b & 0x3F | (b1.toShort & 0xFF) << 6 | (b2.toShort << 14)).toChar
      }
      i += 1
      dst += 1
    }
    new String(cp, 0, dst)
  }

  def encodeFast(s: String): ByteBuffer = {
    val len = s.length()
    // worst case scenario produces 3 bytes per character
    val buf = new Array[Byte](len * 3)
    var src = 0
    var dst = 0
    var c: Char = ' '
    // start by encoding ASCII only
    while ((src < len) && {c = s.charAt(src); c < 0x80}) {
      buf(dst) = c.toByte
      src += 1
      dst += 1
    }

    // did we encode everything?
    if (src == len) {
      ByteBuffer.wrap(buf, 0, len)
    } else {
      // next stage, encode also non-ASCII
      while (src < len) {
        c = s.charAt(src)
        if (c < 0x80) {
          buf(dst) = c.toByte
          dst += 1
        } else if (c < 0x4000) {
          buf(dst) = (0x80 | (c & 0x3F)).toByte
          buf(dst + 1) = (c >> 6 & 0xFF).toByte
          dst += 2
        } else {
          buf(dst) = (0xC0 | (c & 0x3F)).toByte
          buf(dst + 1) = (c >> 6 & 0xFF).toByte
          buf(dst + 2) = (c >> 14).toByte
          dst += 3
        }
        src += 1
      }
      ByteBuffer.wrap(buf, 0, dst)
    }
  }
}

abstract class StringCodecBase extends StringCodecFast {
  def decodeUTF8(len: Int, buf: ByteBuffer): String

  def encodeUTF8(s: String): ByteBuffer

  def decodeUTF16(len: Int, buf: ByteBuffer): String

  def encodeUTF16(s: String): ByteBuffer


}

trait Decoder {
  /**
    * Decodes a single byte
    *
    */
  def readByte: Byte

  /**
    * Decodes a UTF-8 encoded character (1-3 bytes) and produces a single UTF-16 character
    *
    */
  def readChar: Char

  /**
    * Decodes a 16-bit integer
    */
  def readShort: Short

  /**
    * Decodes a 32-bit integer
    */
  def readInt: Int

  def readRawInt: Int

  /**
    * Decodes a 64-bit integer
    */
  def readLong: Long

  def readRawLong: Long

  /**
    * Decodes a 32-bit integer, or returns the first byte if it doesn't contain a valid encoding marker
    */
  def readIntCode: Either[Byte, Int]

  /**
    * Decodes a 64-bit long, or returns the first byte if it doesn't contain a valid encoding marker
    */
  def readLongCode: Either[Byte, Long]

  /**
    * Decodes a 32-bit float (4 bytes)
    */
  def readFloat: Float

  /**
    * Decodes a 64-bit double (8 bytes)
    *
    */
  def readDouble: Double

  /**
    * Decodes a string
    *
    */
  def readString: String

  /**
    * Decodes a string whose length is already known
    *
    * @param len Length of the string (in bytes)
    */
  def readString(len: Int): String

  /**
    * Decodes a ByteBuffer
    */
  def readByteBuffer: ByteBuffer

  /**
    * Decodes an array of Bytes
    */
  def readByteArray(): Array[Byte]
  def readByteArray(len: Int): Array[Byte]

  /**
    * Decodes an array of Integers
    */
  def readIntArray(): Array[Int]
  def readIntArray(len: Int): Array[Int]

  /**
    * Decodes an array of Floats
    */
  def readFloatArray(): Array[Float]
  def readFloatArray(len: Int): Array[Float]

  /**
    * Decodes an array of Doubles
    */
  def readDoubleArray(): Array[Double]
  def readDoubleArray(len: Int): Array[Double]
}

trait Encoder {
  /**
    * Encodes a single byte
    *
    * @param b Byte to encode
    */
  def writeByte(b: Byte): Encoder

  /**
    * Encodes a single character using UTF-8 encoding
    *
    * @param c Character to encode
    */
  def writeChar(c: Char): Encoder

  /**
    * Encodes a short integer
    */
  def writeShort(s: Short): Encoder

  /**
    * Encodes an integer
    */
  def writeInt(i: Int): Encoder

  /**
    * Encodes an integer in 32-bits
    *
    * @param i Integer to encode
    */
  def writeRawInt(i: Int): Encoder

  /**
    * Encodes a long
    *
    * @param l Long to encode
    */
  def writeLong(l: Long): Encoder

  /**
    * Encodes a long in 64-bits
    *
    * @param l Long to encode
    */
  def writeRawLong(l: Long): Encoder

  /**
    * Writes either a code byte (0-15) or an Int
    *
    * @param intCode Integer or a code byte
    */
  def writeIntCode(intCode: Either[Byte, Int]): Encoder

  /**
    * Writes either a code byte (0-15) or a Long
    *
    * @param longCode Long or a code byte
    */
  def writeLongCode(longCode: Either[Byte, Long]): Encoder

  /**
    * Encodes a string
    *
    * @param s String to encode
    */
  def writeString(s: String): Encoder

  /**
    * Encodes a float as 4 bytes
    *
    * @param f Float to encode
    */
  def writeFloat(f: Float): Encoder

  /**
    * Encodes a double as 8 bytes
    *
    * @param d Double to encode
    */
  def writeDouble(d: Double): Encoder

  /**
    * Encodes a ByteBuffer by writing its length and content
    *
    * @param bb ByteBuffer to encode
    */
  def writeByteBuffer(bb: ByteBuffer): Encoder

  /**
    * Encodes an array of Bytes
    */
  def writeByteArray(ba: Array[Byte]): Encoder

  /**
    * Encodes an array of Integers
    */
  def writeIntArray(ia: Array[Int]): Encoder

  /**
    * Encodes an array of Floats
    */
  def writeFloatArray(fa: Array[Float]): Encoder

  /**
    * Encodes an array of Doubles
    */
  def writeDoubleArray(da: Array[Double]): Encoder

  /**
    * Completes the encoding and returns the ByteBuffer
    */
  def asByteBuffer: ByteBuffer

  /**
    * Completes the encoding and returns a sequence of ByteBuffers
    */
  def asByteBuffers: Iterable[ByteBuffer]
}
