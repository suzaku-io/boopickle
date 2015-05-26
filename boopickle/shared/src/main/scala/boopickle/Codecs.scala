package boopickle

import java.nio.{ByteOrder, ByteBuffer}
import java.nio.charset.{CharacterCodingException, StandardCharsets}

class Decoder(val buf: ByteBuffer) {
  /**
   * Decodes a single byte
   * @return
   */
  @inline def readByte: Byte = {
    buf.get
  }

  /**
   * Decodes a UTF-8 encoded character (1-3 bytes) and produces a single UTF-16 character
   * @return
   */
  def readChar: Char = {
    val b0 = buf.get & 0xFF
    if (b0 < 0x80)
      b0.toChar
    else if ((b0 & 0xE0) == 0xC0) {
      val b1 = buf.get & 0x3F
      ((b0 & 0x1F) << 6 | b1).toChar
    } else if ((b0 & 0xF0) == 0xE0) {
      val s0 = buf.get & 0x3F
      val s1 = buf.get & 0x3F
      ((b0 & 0x0F) << 12 | s0 << 6 | s1).toChar
    } else
      throw new CharacterCodingException
  }

  /**
   * Decodes a 32-bit integer (1-5 bytes)
   * <pre>
   * 0XXX XXXX                            = 0 to 127
   * 1000 XXXX  b0                        = 128 to 4095
   * 1001 XXXX  b0                        = -1 to -4095
   * 1010 XXXX  b0 b1                     = 4096 to 1048575
   * 1011 XXXX  b0 b1                     = -4096 to -1048575
   * 1100 XXXX  b0 b1 b2                  = 1048576 to 268435455
   * 1101 XXXX  b0 b1 b2                  = -1048576 to -268435455
   * 1110 0000  b0 b1 b2 b3               = MinInt to MaxInt
   * 1111 ????                            = reserved for special codings
   * </pre>
   * @return
   */
  def readInt: Int = {
    val b = buf.get & 0xFF
    if ((b & 0x80) != 0) {
      // special coding, expand sign bit
      val sign = if ((b & 0x10) == 0) 1 else -1
      val b0 = b & 0xF
      b >> 4 match {
        case 0x8 | 0x9 =>
          val b1 = buf.get & 0xFF
          sign * (b0 << 8 | b1)
        case 0xA | 0xB =>
          val b1 = buf.get & 0xFF
          val b2 = buf.get & 0xFF
          sign * (b0 << 16 | b1 << 8 | b2)
        case 0xC | 0xD =>
          val b1 = buf.get & 0xFF
          val b2 = buf.get & 0xFF
          val b3 = buf.get & 0xFF
          sign * (b0 << 24 | b1 << 16 | b2 << 8 | b3)
        case 0xE if b == 0xE0 =>
          sign * readRawInt
        case _ =>
          throw new IllegalArgumentException("Unknown integer coding")
      }
    } else {
      b
    }
  }

  @inline def readRawInt: Int = {
    buf.getInt
  }

  /**
   * Decodes a 64-bit integer (1-9 bytes)
   * <pre>
   * 0XXX XXXX                            = 0 to 127
   * 1000 XXXX  b0                        = 128 to 4095
   * 1001 XXXX  b0                        = -1 to -4095
   * 1010 XXXX  b0 b1                     = 4096 to 1048575
   * 1011 XXXX  b0 b1                     = -4096 to -1048575
   * 1100 XXXX  b0 b1 b2                  = 1048576 to 268435455
   * 1101 XXXX  b0 b1 b2                  = -1048576 to -268435455
   * 1110 0000  b0 b1 b2 b3               = MinInt to MaxInt
   * 1110 0001  b0 b1 b2 b3 b4 b5 b6 b7   = anything larger
   * 1111 ????                            = reserved for special codings
   * </pre>
   * @return
   */
  def readLong: Long = {
    val b = buf.get & 0xFF
    if (b != 0xE1) {
      buf.position(buf.position - 1)
      readInt
    } else {
      readRawLong
    }
  }

  @inline def readRawLong: Long = {
    buf.getLong
  }

  /**
   * Decodes a 32-bit integer, or returns the first byte if it doesn't contain a valid encoding marker
   * @return
   */
  def readIntCode: Either[Byte, Int] = {
    val b = buf.get & 0xFF
    if ((b & 0x80) != 0) {
      // special coding, expand sign bit
      val sign = if ((b & 0x10) == 0) 1 else -1
      val b0 = b & 0xF
      b >> 4 match {
        case 0x8 | 0x9 =>
          val b1 = buf.get & 0xFF
          Right(sign * (b0 << 8 | b1))
        case 0xA | 0xB =>
          val b1 = buf.get & 0xFF
          val b2 = buf.get & 0xFF
          Right(sign * (b0 << 16 | b1 << 8 | b2))
        case 0xC | 0xD =>
          val b1 = buf.get & 0xFF
          val b2 = buf.get & 0xFF
          val b3 = buf.get & 0xFF
          Right(sign * (b0 << 24 | b1 << 16 | b2 << 8 | b3))
        case 0xE if b == 0xE0 =>
          Right(sign * readRawInt)
        case _ =>
          Left(b.toByte)
      }
    } else {
      Right(b)
    }
  }

  /**
   * Decodes a 64-bit long, or returns the first byte if it doesn't contain a valid encoding marker
   * @return
   */
  def readLongCode: Either[Byte, Long] = {
    val b = buf.get & 0xFF
    if (b != 0xE1) {
      buf.position(buf.position - 1)
      readIntCode match {
        case Left(x) => Left(x)
        case Right(x) => Right(x.toLong)
      }
    } else
      Right(readRawLong)
  }

  /**
   * Decodes a 32-bit float (4 bytes)
   * @return
   */
  @inline def readFloat: Float = {
    buf.getFloat
  }

  /**
   * Decodes a 64-bit double (8 bytes)
   * @return
   */
  @inline def readDouble: Double = {
    buf.getDouble
  }

  /**
   * Decodes a UTF-8 encoded string
   *
   * @return
   */
  def readString: String = {
    // read string length
    val len = readInt
    StringCodec.decodeUTF8(len, buf)
  }

  /**
   * Decodes a UTF-8 encoded string whose length is already known
   * @param len Length of the string (in bytes)
   * @return
   */
  def readString(len: Int): String = {
    StringCodec.decodeUTF8(len, buf)
  }

  def readByteBuffer: ByteBuffer = {
    val size = readInt
    if (size < 0)
      throw new IllegalArgumentException(s"Invalid size $size for ByteBuffer")

    // create a copy (sharing content), enforce little endian
    val b = buf.slice().order(ByteOrder.LITTLE_ENDIAN)
    buf.position(buf.position + size)
    b.limit(b.position + size)
    b
  }
}

class Encoder(bufferProvider: BufferProvider = DefaultByteBufferProvider.provider) {

  @inline private def alloc(size: Int): ByteBuffer = bufferProvider.alloc(size)

  /**
   * Encodes a single byte
   * @param b Byte to encode
   * @return
   */
  @inline def writeByte(b: Byte): Encoder = {
    alloc(1).put(b)
    this
  }

  /**
   * Encodes a single character using UTF-8 encoding
   *
   * @param c Character to encode
   * @return
   */
  def writeChar(c: Char): Encoder = {
    if (c < 0x80) {
      alloc(1).put(c.toByte)
    } else if (c < 0x800) {
      alloc(2).put((0xC0 | (c >>> 6 & 0x3F)).toByte).put((0x80 | (c & 0x3F)).toByte)
    } else {
      alloc(3).put((0xE0 | (c >>> 12)).toByte).put((0x80 | (c >>> 6 & 0x3F)).toByte).put((0x80 | (c & 0x3F)).toByte)
    }
    this
  }

  /**
   * Encodes an integer efficiently in 1 to 5 bytes
   * <pre>
   * 0XXX XXXX                            = 0 to 127
   * 1000 XXXX  b0                        = 128 to 4095
   * 1001 XXXX  b0                        = -1 to -4095
   * 1010 XXXX  b0 b1                     = 4096 to 1048575
   * 1011 XXXX  b0 b1                     = -4096 to -1048575
   * 1100 XXXX  b0 b1 b2                  = 1048575 to 268435455
   * 1101 XXXX  b0 b1 b2                  = -1048575 to -268435455
   * 1110 0000  b0 b1 b2 b3               = MinInt to MaxInt
   * 1111 ????                            = reserved for special codings
   * </pre>
   * @param i Integer to encode
   */
  def writeInt(i: Int): Encoder = {
    // check for a short number
    if (i >= 0 && i < 128) {
      alloc(1).put(i.toByte)
    } else {
      if (i > -268435456 && i < 268435456) {
        val mask = i >>> 31 << 4
        val a = Math.abs(i)
        if (a < 4096) {
          alloc(2).put((mask | 0x80 | (a >> 8)).toByte).put((a & 0xFF).toByte)
        } else if (a < 1048576) {
          alloc(3).put((mask | 0xA0 | (a >> 16)).toByte).put(((a >> 8) & 0xFF).toByte).put((a & 0xFF).toByte)
        } else {
          alloc(4).put((mask | 0xC0 | (a >> 24)).toByte).put(((a >> 16) & 0xFF).toByte).put(((a >> 8) & 0xFF).toByte).put((a & 0xFF).toByte)
        }
      } else {
        alloc(5).put(0xE0.toByte).putInt(i)
      }
    }
    this
  }

  /**
   * Encodes an integer in 32-bits
   * @param i Integer to encode
   * @return
   */
  def writeRawInt(i: Int): Encoder = {
    alloc(4).putInt(i)
    this
  }

  /**
   * Encodes a long efficiently in 1 to 9 bytes
   * <pre>
   * 0XXX XXXX                            = 0 to 127
   * 1000 XXXX  b0                        = 128 to 4095
   * 1001 XXXX  b0                        = -1 to -4096
   * 1010 XXXX  b0 b1                     = 4096 to 1048575
   * 1011 XXXX  b0 b1                     = -4096 to -1048575
   * 1100 XXXX  b0 b1 b2                  = 1048576 to 268435455
   * 1101 XXXX  b0 b1 b2                  = -1048576 to -268435455
   * 1110 0000  b0 b1 b2 b3               = MinInt to MaxInt
   * 1110 0001  b0 b1 b2 b3 b4 b5 b6 b7   = anything larger
   * 1111 ????                            = reserved for special codings
   * </pre>
   * @param l Long to encode
   */
  def writeLong(l: Long): Encoder = {
    if (l <= Int.MaxValue && l >= Int.MinValue)
      writeInt(l.toInt)
    else {
      alloc(9).put(0xE1.toByte).putLong(l)
    }
    this
  }

  /**
   * Encodes a long in 64-bits
   * @param l Long to encode
   * @return
   */
  def writeRawLong(l: Long): Encoder = {
    alloc(8).putLong(l)
    this
  }
  /**
   * Encodes a string using UTF8
   *
   * @param s String to encode
   * @return
   */
  def writeString(s: String): Encoder = {
    val strBytes = StringCodec.encodeUTF8(s)
    writeInt(strBytes.limit)
    alloc(strBytes.limit).put(strBytes)
    this
  }

  /**
   * Encodes a float as 4 bytes
   *
   * @param f Float to encode
   * @return
   */
  @inline def writeFloat(f: Float): Encoder = {
    alloc(4).putFloat(f)
    this
  }

  /**
   * Encodes a double as 8 bytes
   *
   * @param d Double to encode
   * @return
   */
  @inline def writeDouble(d: Double): Encoder = {
    alloc(8).putDouble(d)
    this
  }

  /**
   * Encodes a ByteBuffer by writing its length and content
   *
   * @param bb ByteBuffer to encode
   * @return
   */
  def writeByteBuffer(bb: ByteBuffer): Encoder = {
    bb.mark()
    writeInt(bb.remaining).alloc(bb.remaining).put(bb)
    bb.reset()
    this
  }

  /**
   * Completes the encoding and returns the ByteBuffer
   * @return
   */
  def asByteBuffer = bufferProvider.asByteBuffer

  /**
   * Completes the encoding and returns a sequence of ByteBuffers
   * @return
   */
  def asByteBuffers = bufferProvider.asByteBuffers
}