package boopickle

import java.nio.{ByteBuffer, ByteOrder}

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
}

abstract class ByteBufferProvider extends BufferProvider {
  import ByteBufferProvider._
  protected var buffers: List[ByteBuffer] = Nil
  protected var currentBuf: ByteBuffer = allocate(initSize)

  protected def allocate(size: Int): ByteBuffer

  def alloc(size: Int): ByteBuffer = {
    if (currentBuf.remaining() < size) {
      // flip current buffer (prepare for reading and set limit)
      currentBuf.flip()
      buffers = currentBuf :: buffers
      // replace current buffer with the new one
      currentBuf = allocate((size + expandSize + 15) & ~15)
    }
    currentBuf
  }

  def asByteBuffer = {
    currentBuf.flip()
    if (buffers.isEmpty)
      currentBuf
    else {
      val bufList = (currentBuf :: buffers).reverse
      // create a new buffer and combine all buffers into it
      val comb = allocate(bufList.map(_.limit).sum)
      bufList.foreach(buf => comb.put(buf))
      comb.flip()
      comb
    }
  }

  def asByteBuffers = {
    currentBuf.flip()
    (currentBuf :: buffers).toVector
  }
}

object ByteBufferProvider {
  final val initSize = 512
  final val expandSize = initSize * 4
}

class HeapByteBufferProvider extends ByteBufferProvider {
  override protected def allocate(size: Int) = {
    BufferPool.allocate(size).getOrElse(ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN))
  }

  override def asByteBuffer = {
    currentBuf.flip()
    if (buffers.isEmpty)
      currentBuf
    else {
      // create a new buffer and combine all buffers into it
      val bufList = (currentBuf :: buffers).reverse
      val comb = allocate(bufList.map(_.limit).sum)
      bufList.foreach {buf =>
        comb.put(buf)
        // release to the pool
        BufferPool.release(buf)
      }
      comb.flip()
      comb
    }
  }

}

class DirectByteBufferProvider extends ByteBufferProvider {
  override protected def allocate(size: Int) = {
    BufferPool.allocateDirect(size).getOrElse(ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN))
  }

  override def asByteBuffer = {
    currentBuf.flip()
    if (buffers.isEmpty)
      currentBuf
    else {
      // create a new buffer and combine all buffers into it
      val bufList = (currentBuf :: buffers).reverse
      val comb = allocate(bufList.map(_.limit).sum)
      bufList.foreach {buf =>
        comb.put(buf)
        // release to the pool
        BufferPool.release(buf)
      }
      comb.flip()
      comb
    }
  }
}

trait DefaultByteBufferProviderFuncs {
  def provider: ByteBufferProvider
}
