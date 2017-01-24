package boopickle.perftests

import java.nio.charset.StandardCharsets

import upickle.default._

abstract class UPickleRunner[A](data: A) extends TestRunner[A](data) {
  override def name = "uPickle"
}

object UPickleRunners {

  def encodeRunner[A](data: A)(implicit p: Writer[A], u: Reader[A]): UPickleRunner[A] = new UPickleRunner[A](data) {
    var testData: A = _

    override def initialize = {
      testData = data
      val res = write(testData)
      // println(res)
      res.getBytes(StandardCharsets.UTF_8)
    }

    override def run(): Unit = {
      write(testData)
      ()
    }
  }

  def decodeRunner[A](data: A)(implicit p: Writer[A], u: Reader[A]): UPickleRunner[A] = new UPickleRunner[A](data) {
    var testData: A = _
    var s: String   = _

    override def initialize = {
      testData = data
      s = write(testData)
      s.getBytes(StandardCharsets.UTF_8)
    }

    override def run(): Unit = {
      read[A](s)
      ()
    }
  }
}
