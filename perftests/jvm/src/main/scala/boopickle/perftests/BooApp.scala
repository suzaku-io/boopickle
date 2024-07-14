package boopickle.perftests

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

import boopickle.BufferPool

object BooApp {

  def main(args: Array[String]): Unit = {
    runTests()
  }

  def runTests(): Unit = {
    Tests.suites.zipWithIndex.foreach { case (suite, idx) =>
      val header = s"${1 + idx}/${Tests.suites.size} : ${suite.name}"
      println(header)
      println("=" * header.length)
      println(f"${"Library"}%-10s ${"ops/s"}%-10s ${"%"}%-10s ${"size"}%-10s ${"%"}%-10s ${"size.gz"}%-10s ${"%"}%-10s")
      val tester = new PerfTester(suite)
      val res    = tester.runSuite
      // zip result data to see how small it gets
      val resSizes = res.results.map { r =>
        val rawSize = r.data.length
        val bs      = new ByteArrayOutputStream()
        val gs      = new GZIPOutputStream(bs)
        gs.write(r.data)
        gs.finish()
        bs.flush()
        val gzipped = bs.toByteArray.length
        (r, rawSize, gzipped)
      }
      val maxCount  = resSizes.map(_._1.count).max
      val minSize   = resSizes.map(_._2).min
      val minGZSize = resSizes.map(_._3).min
      resSizes.foreach { r =>
        println(
          f"${r._1.name}%-10s ${r._1.count}%-10d ${f"${r._1.count * 100.0 / maxCount}%.1f%%"}%-10s ${r._2}%-10d ${f"${r._2 * 100.0 / minSize}%.0f%%"}%-10s ${r._3}%-10d ${f"${r._3 * 100.0 / minGZSize}%.0f%%"}%-10s"
        )
      }
      println()
      // print out buffer pool usage
      println(s"""BufferPool:
           |  allocations = ${BufferPool.allocOk}
           |  misses      = ${BufferPool.allocMiss}
           """.stripMargin)
    }
  }
}
