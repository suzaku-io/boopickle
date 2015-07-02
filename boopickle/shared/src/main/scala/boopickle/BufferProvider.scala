package boopickle

import java.nio.{ByteOrder, ByteBuffer}

import scala.collection.mutable

trait BufferProvider {
  /**
   * Makes sure the ByteBuffer has enough space for new data. If not, allocates a new ByteBuffer
   * and returns it. The returned ByteBuffer must have little-endian ordering.
   *
   * @param size Number of bytes needed for new data
   * @return
   */
  def alloc(size: Int): ByteBuffer

  /**
   * Completes the encoding and returns the ByteBuffer, merging the chain of buffers if necessary
   *
   * @return
   */
  def asByteBuffer: ByteBuffer

  /**
   * Completes the encoding and returns a sequence of ByteBuffers
   *
   * @return
   */
  def asByteBuffers: Iterable[ByteBuffer]

  /**
   * Resets the buffer provider so it can be reused
   */
  def reset(): Unit
}

abstract class ByteBufferProvider extends BufferProvider {
  // fits into a MTU
  private final val initSize = 1400
  private val buffers = mutable.ArrayBuffer[ByteBuffer]()
  private var currentBuf: ByteBuffer = _

  // prepare initial buffer
  reset()

  protected def allocate(size: Int): ByteBuffer

  def alloc(size: Int): ByteBuffer = {
    if (currentBuf.remaining() < size) {
      val newBuf = allocate(size + initSize * 2)
      buffers.append(newBuf)
      // flip current buffer (prepare for reading and set limit)
      currentBuf.flip()
      // replace current buffer with the new one
      currentBuf = newBuf
    }
    currentBuf
  }

  def asByteBuffer = {
    currentBuf.flip()
    if (buffers.size == 1)
      currentBuf
    else {
      // create a new buffer and combine all buffers into it
      val comb = allocate(buffers.map(_.limit).sum)
      buffers.foreach(buf => comb.put(buf))
      comb.flip()
      comb
    }
  }

  def asByteBuffers = {
    currentBuf.flip()
    buffers.toVector
  }

  def reset(): Unit = {
    buffers.clear()
    currentBuf = allocate(initSize)
    buffers += currentBuf
  }
}

class HeapByteBufferProvider extends ByteBufferProvider {
  override protected def allocate(size: Int) = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
}

class DirectByteBufferProvider extends ByteBufferProvider {
  override protected def allocate(size: Int) = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN)
}