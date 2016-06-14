package boopickle

import java.nio.ByteBuffer
import java.util.Comparator
import java.util.concurrent.ConcurrentSkipListSet

object BufferPool {
  case class Entry(bb: ByteBuffer, size: Int)

  class EntryComparator extends Comparator[Entry] {
    // reversed order so that largest size is first
    override def compare(o1: Entry, o2: Entry): Int = o2.size - o1.size
  }

  // maximum size of a ByteBuffer to be included in the pool
  private final val maxBufferSize = 64*1024
  // maximum total size of buffers in the pool
  private final val maxPoolSize = 4*1024*1024

  val pool = new ConcurrentSkipListSet[Entry](new EntryComparator)
  // for collecting some performance characteristics
  @volatile var allocOk = 0
  @volatile var allocMiss = 0
  @volatile var maxSize = 0L
  @volatile var poolSize = 0L
  @volatile var poolCount = 0

  def allocate(minSize: Int): Option[ByteBuffer] = {
    pool.synchronized {
      if (pool.isEmpty) {
        allocMiss += 1
        None
      } else {
        val e = pool.first
        if (e.size >= minSize) {
          allocOk += 1
          poolSize -= e.size
          poolCount -= 1
          pool.remove(e)
          Some(e.bb)
        } else {
          allocMiss += 1
          None
        }
      }
    }
  }

  def release(bb: ByteBuffer): Unit = {
    // do not take large buffers into the pool, as their reallocation is relatively cheap
    if(bb.capacity < maxBufferSize && poolSize + bb.capacity() < maxPoolSize) {
      bb.clear()
      pool.add(Entry(bb, bb.capacity()))
      poolSize += bb.capacity
      poolCount += 1
      maxSize = maxSize max poolSize
    }
  }
}
