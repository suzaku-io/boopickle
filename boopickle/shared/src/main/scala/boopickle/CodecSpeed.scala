package boopickle

import java.nio.charset.CharacterCodingException
import java.nio.{ByteBuffer, ByteOrder}

class DecoderSpeed(val buf: ByteBuffer) extends Decoder {
  val stringCodec: StringCodecFuncs = StringCodec
  /**
    * Decodes a single byte
    * @return
    */
  def readByte: Byte = {
    buf.get
  }

  /**
    * Decodes a character
    * @return
    */
  def readChar: Char = {
    buf.getChar()
  }

  /**
    * Decodes a 16-bit integer
    */
  def readShort: Short = {
    buf.getShort
  }

  /**
    * Decodes a 32-bit integer
    */
  def readInt: Int = {
    buf.getInt
  }

  def readRawInt: Int = {
    buf.getInt
  }

  /**
    * Decodes a 64-bit integer
    */
  def readLong: Long = {
    buf.getLong
  }

  def readRawLong: Long = {
    buf.getLong
  }

  /**
    * Decodes a 32-bit integer, or a special code
    * @return
    */
  def readIntCode: Either[Byte, Int] = {
    val b = buf.get & 0xFF
    if ((b & 0x80) != 0) {
      Left((b & 0xF).toByte)
    } else {
      Right(buf.getInt)
    }
  }

  /**
    * Decodes a 64-bit long, or a special code
    * @return
    */
  def readLongCode: Either[Byte, Long] = {
    val b = buf.get & 0xFF
    if ((b & 0x80) != 0) {
      Left((b & 0xF).toByte)
    } else {
      Right(buf.getLong)
    }
  }

  /**
    * Decodes a 32-bit float (4 bytes)
    * @return
    */
  def readFloat: Float = {
    buf.getFloat
  }

  /**
    * Decodes a 64-bit double (8 bytes)
    * @return
    */
  def readDouble: Double = {
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
    stringCodec.decodeUTF16(len, buf)
  }

  /**
    * Decodes a UTF-8 encoded string whose length is already known
    * @param len Length of the string (in bytes)
    * @return
    */
  def readString(len: Int): String = {
    stringCodec.decodeUTF16(len, buf)
  }

  def readByteBuffer: ByteBuffer = {
    // length and byte order are encoded into same integer
    val sizeBO = readInt
    if (sizeBO < 0)
      throw new IllegalArgumentException(s"Invalid size $sizeBO for ByteBuffer")
    val size = sizeBO >> 1
    val byteOrder = if((sizeBO & 1) == 1) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
    // create a copy (sharing content), set correct byte order
    val b = buf.slice().order(byteOrder)
    buf.position(buf.position + size)
    b.limit(b.position + size)
    b
  }
}

class EncoderSpeed(bufferProvider: BufferProvider = DefaultByteBufferProvider.provider) extends Encoder {
  val stringCodec: StringCodecFuncs = StringCodec

  private def alloc(size: Int): ByteBuffer = bufferProvider.alloc(size)

  /**
    * Encodes a single byte
    * @param b Byte to encode
    * @return
    */
  def writeByte(b: Byte): Encoder = {
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
    alloc(2).putChar(c)
    this
  }

  /**
    * Encodes a short integer
    */
  def writeShort(s: Short): Encoder = {
    alloc(2).putShort(s)
    this
  }

  /**
    * Encodes an integer
    * @param i Integer to encode
    */
  def writeInt(i: Int): Encoder = {
    alloc(4).putInt(i)
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
    * Encodes a long
    * @param l Long to encode
    */
  def writeLong(l: Long): Encoder = {
    alloc(8).putLong(l)
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
    * Writes either a code byte (0-15) or an Int
    * @param intCode Integer or a code byte
    */
  def writeIntCode(intCode: Either[Byte, Int]): Encoder = {
    intCode match {
      case Left(code) =>
        alloc(1).put((code | 0x80).toByte)
      case Right(i) =>
        alloc(5).put(0.toByte).putInt(i)
    }
    this
  }

  /**
    * Writes either a code byte (0-15) or a Long
    * @param longCode Long or a code byte
    */
  def writeLongCode(longCode: Either[Byte, Long]): Encoder = {
    longCode match {
      case Left(code) =>
        alloc(1).put((code | 0x80).toByte)
      case Right(l) =>
        alloc(9).put(0.toByte).putLong(l)
    }
    this
  }

  /**
    * Encodes a string using UTF8
    *
    * @param s String to encode
    * @return
    */
  def writeString(s: String): Encoder = {
    val strBytes = stringCodec.encodeUTF16(s)
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
  def writeFloat(f: Float): Encoder = {
    alloc(4).putFloat(f)
    this
  }

  /**
    * Encodes a double as 8 bytes
    *
    * @param d Double to encode
    * @return
    */
  def writeDouble(d: Double): Encoder = {
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
    val byteOrder = if(bb.order() == ByteOrder.BIG_ENDIAN) 1 else 0
    // encode byte order as bit 0 in the length
    writeInt(bb.remaining * 2 | byteOrder )
    alloc(bb.remaining).put(bb)
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