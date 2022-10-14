package boopickle

import java.nio.{ByteBuffer, ByteOrder}

import utest._

object BufferPoolTestsJVM extends TestSuite {

  override def tests = Tests {
    "MultiThread" - {
      val pool  = BufferPool
      val count = 100000
      def runner = new Runnable {
        override def run(): Unit = {
          var i = 0
          while (i < count) {
            val bb1 = pool
              .allocate(ByteBufferProvider.initSize)
              .getOrElse(ByteBuffer.allocate(ByteBufferProvider.initSize))
              .order(ByteOrder.LITTLE_ENDIAN)
            val bb2 = pool
              .allocate(ByteBufferProvider.expandSize)
              .getOrElse(ByteBuffer.allocate(ByteBufferProvider.expandSize))
              .order(ByteOrder.LITTLE_ENDIAN)
            pool.release(bb1)
            pool.release(bb2)
            pool.release(ByteBuffer.allocate(ByteBufferProvider.initSize).order(ByteOrder.LITTLE_ENDIAN))
            i += 1
          }
        }
      }
      // warmup
      runner.run()
      runner.run()
      System.gc()
      // run in a single thread
      var startTime = System.nanoTime()
      runner.run()
      var endTime = System.nanoTime()
      println(s"Single thread: ${(endTime - startTime) / 1000}")
      var t1 = new Thread(runner)
      var t2 = new Thread(runner)
      startTime = System.nanoTime()
      t1.start()
      t2.start()
      t1.join()
      t2.join()
      endTime = System.nanoTime()
      println(s"Two threads: ${(endTime - startTime) / 1000}")
      startTime = System.nanoTime()
      t1 = new Thread(runner)
      t2 = new Thread(runner)
      val t3 = new Thread(runner)
      t1.start()
      t2.start()
      t3.start()
      t1.join()
      t2.join()
      t3.join()
      endTime = System.nanoTime()
      println(s"Three threads: ${(endTime - startTime) / 1000}")
    }
  }
}
