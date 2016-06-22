package boopickle

import java.nio.ByteBuffer

object BufferPool {

  private final case class Entry(bb: ByteBuffer, size: Int)

  // two pools for two different size categories
  private final val poolEntrySize0 = ByteBufferProvider.initSize
  private final val poolEntrySize1 = ByteBufferProvider.expandSize + 16
  // maximum size of a ByteBuffer to be included in a pool
  private final val maxBufferSize = poolEntrySize1 * 2
  // maximum total size of buffers in a pool
  private final val maxPoolSize = 4 * 1024 * 1024

  var disablePool = false

  final class Pool {
    @volatile private var pool0 = List.empty[Entry]
    @volatile private var pool1 = List.empty[Entry]

    // for collecting some performance characteristics
    @volatile var allocOk = 0
    @volatile var allocMiss = 0
    @volatile var maxSize = 0
    @volatile var poolSize = 0
    @volatile var poolCount = 0

    def allocate(minSize: Int): Option[ByteBuffer] = {
      if (disablePool)
        None
      else if (minSize > poolEntrySize1 || poolCount == 0) {
        allocMiss += 1
        None
      } else if (minSize > poolEntrySize0 || pool0.isEmpty) {
        this.synchronized {
          if (pool1.isEmpty) {
            allocMiss += 1
            None
          } else {
            val e = pool1.head
            allocOk += 1
            poolSize -= e.size
            poolCount -= 1
            pool1 = pool1.tail
            Some(e.bb)
          }
        }
      } else {
        this.synchronized {
          val e = pool0.head
          allocOk += 1
          poolSize -= e.size
          poolCount -= 1
          pool0 = pool0.tail
          Some(e.bb)
        }
      }
    }

    def release(bb: ByteBuffer): Unit = {
      if (!disablePool) {
        // do not take large buffers into the pool, as their reallocation is relatively cheap
        val bufSize = bb.capacity
        if (bufSize < maxBufferSize && poolSize + bufSize < maxPoolSize && bufSize >= poolEntrySize0) {
          bb.clear()
          this.synchronized {
            if (bufSize >= poolEntrySize1) {
              pool1 = Entry(bb, bufSize) :: pool1
            } else {
              pool0 = Entry(bb, bufSize) :: pool0
            }
            poolSize += bufSize
            poolCount += 1
            maxSize = maxSize max poolSize
          }
        }
      }
    }
  }

  val heapPool = new Pool
  val directPool = new Pool

  def allocate(minSize: Int): Option[ByteBuffer] = {
    heapPool.allocate(minSize)
  }

  def allocateDirect(minSize: Int): Option[ByteBuffer] = {
    directPool.allocate(minSize)
  }

  def release(bb: ByteBuffer): Unit = {
    if (bb.isDirect)
      directPool.release(bb)
    else
      heapPool.release(bb)
  }

  def allocOk = heapPool.allocOk + directPool.allocOk
  def allocMiss = heapPool.allocMiss + directPool.allocMiss
  def maxSize = heapPool.maxSize + directPool.maxSize
  def poolSize = heapPool.poolSize + directPool.poolSize
  def poolCount = heapPool.poolCount + directPool.poolCount
}
