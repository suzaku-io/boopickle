package boopickle

import java.nio.{ByteBuffer, ByteOrder}
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.{AtomicInteger, AtomicReferenceArray}
import java.util.concurrent.locks.Lock

/*
object BufferPool {

  // two pools for two different size categories
  private final val poolEntrySize0 = ByteBufferProvider.initSize
  private final val poolEntrySize1 = ByteBufferProvider.expandSize + 16
  // maximum size of a ByteBuffer to be included in a pool
  private final val maxBufferSize = poolEntrySize1 * 2
  private final val entryCount = 1024

  private var disablePool = false

  final class Pool {
    private val pool0 = new Array[ByteBuffer](entryCount)
    private val pool0lock = new Semaphore(1)
    @volatile private var allocIdx0 = 0
    @volatile private var releaseIdx0 = 0
    private val pool1 = new Array[ByteBuffer](entryCount)
    private val pool1lock = new Semaphore(1)
    @volatile private var allocIdx1 = 0
    @volatile private var releaseIdx1 = 0

    // for collecting some performance characteristics
    var allocOk = 0
    var allocMiss = 0

    def allocate(minSize: Int): Option[ByteBuffer] = {
      if (disablePool) {
        None
      } else if (minSize > poolEntrySize1) {
        allocMiss += 1
        None
      } else if (minSize > poolEntrySize0) {
        // allocate from pool1
        if(pool1lock.tryAcquire()) {
          val res = if (allocIdx1 != releaseIdx1) {
            // allocate
            allocIdx1 = (allocIdx1 + 1) % entryCount
            Some(pool1(allocIdx1))
          } else {
            allocMiss += 1
            None
          }
          pool1lock.release()
          res
        } else {
          allocMiss += 1
          None
        }
      } else {
        // allocate from pool0
        if(pool0lock.tryAcquire()) {
          val res = if (allocIdx0 != releaseIdx0) {
            // allocate
            allocIdx0 = (allocIdx0 + 1) % entryCount
            Some(pool0(allocIdx0))
          } else {
            allocMiss += 1
            None
          }
          pool0lock.release()
          res
        } else {
          allocMiss += 1
          None
        }
      }
    }

    def release(bb: ByteBuffer): Unit = {
      if (!disablePool) {
        // do not take large buffers into the pool, as their reallocation is relatively cheap
        val bufSize = bb.capacity
        if (bufSize < maxBufferSize && bufSize >= poolEntrySize0) {
          bb.clear()
          if (bufSize >= poolEntrySize1) {
            if(pool1lock.tryAcquire()) {
              val rNext = (releaseIdx1 + 1) % entryCount
              if (rNext != allocIdx1) {
                // release the buffer
                releaseIdx1 = rNext
                pool1(rNext) = bb
              }
              pool1lock.release()
            }
          } else {
            if(pool0lock.tryAcquire()) {
              val rNext = (releaseIdx0 + 1) % entryCount
              if (rNext != allocIdx0) {
                // try to release the buffer
                releaseIdx0 = rNext
                pool0(rNext) = bb
              }
              pool0lock.release()
            }
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

  def disable(): Unit = disablePool = true

  def enable(): Unit = disablePool = false

  def allocOk = heapPool.allocOk + directPool.allocOk
  def allocMiss = heapPool.allocMiss + directPool.allocMiss
}
*/

object BufferPool {

  // two pools for two different size categories
  private final val poolEntrySize0 = ByteBufferProvider.initSize
  private final val poolEntrySize1 = ByteBufferProvider.expandSize + 16
  // maximum size of a ByteBuffer to be included in a pool
  private final val maxBufferSize = poolEntrySize1 * 2
  private final val entryCount = 1024

  private var disablePool = false

  final class Pool {
    private val pool0 = new Array[ByteBuffer](entryCount)
    private val pool1 = new Array[ByteBuffer](entryCount)
    private val allocIdx0 = new AtomicInteger(0)
    private val allocIdx1 = new AtomicInteger(0)
    private val releaseIdx0 = new AtomicInteger(0)
    private val releaseIdx1 = new AtomicInteger(0)

    // for collecting some performance characteristics
    var allocOk = 0
    var allocMiss = 0

    def allocate(minSize: Int): Option[ByteBuffer] = {
      if (disablePool) {
        None
      } else if (minSize > poolEntrySize1) {
        allocMiss += 1
        None
      } else if (minSize > poolEntrySize0) {
        // allocate from pool1
        val aIdx = allocIdx1.get()
        val rIdx = releaseIdx1.get()
        val aNext = (aIdx + 1) % entryCount
        if (aIdx != rIdx) {
          // try to allocate
          val result = Some(pool1(aNext))
          if (allocIdx1.compareAndSet(aIdx, aNext)) {
            allocOk += 1
            result
          } else {
            allocMiss += 1
            None
          }
        } else {
          allocMiss += 1
          None
        }
      } else {
        // allocate from pool0
        val aIdx = allocIdx0.get()
        val rIdx = releaseIdx0.get()
        val aNext = (aIdx + 1) % entryCount
        if (aIdx != rIdx) {
          // try to allocate
          val result = Some(pool0(aNext))
          if (allocIdx0.compareAndSet(aIdx, aNext)) {
            allocOk += 1
            result
          } else {
            allocMiss += 1
            None
          }
        } else {
          allocMiss += 1
          None
        }
      }
    }

    def release(bb: ByteBuffer): Unit = {
      if (!disablePool) {
        // do not take large buffers into the pool, as their reallocation is relatively cheap
        val bufSize = bb.capacity
        if (bufSize < maxBufferSize && bufSize >= poolEntrySize0) {
          if (bufSize >= poolEntrySize1) {
            val aIdx = allocIdx1.get()
            val rIdx = releaseIdx1.get()
            val rNext = (rIdx + 1) % entryCount
            if (rNext != aIdx) {
              // try to release the buffer
              bb.clear()
              pool1(rNext) = bb
              releaseIdx1.compareAndSet(rIdx, rNext)
            }
          } else {
            val aIdx = allocIdx0.get()
            val rIdx = releaseIdx0.get()
            val rNext = (rIdx + 1) % entryCount
            if (rNext != aIdx) {
              // try to release the buffer
              bb.clear()
              pool0(rNext) = bb
              releaseIdx0.compareAndSet(rIdx, rNext)
            }
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

  def disable(): Unit = disablePool = true

  def enable(): Unit = disablePool = false

  def allocOk = heapPool.allocOk + directPool.allocOk
  def allocMiss = heapPool.allocMiss + directPool.allocMiss
}
