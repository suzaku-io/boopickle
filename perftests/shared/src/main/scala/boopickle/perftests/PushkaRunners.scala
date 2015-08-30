package boopickle.perftests

import java.nio.charset.StandardCharsets

import pushka.{Reader, Writer}
import pushka.json._

abstract class PushkaRunner[A](data: A) extends TestRunner[A](data) {
  override def name = "Pushka"
}

object PushkaRunners {

  def encodeRunner[A](data:A)(implicit p: Writer[A], u:Reader[A]):PushkaRunner[A] = new PushkaRunner[A](data) {
    var testData : A = _

    override def initialize = {
      testData = data
      val res = write(testData)
      res.getBytes(StandardCharsets.UTF_8)
    }

    override def run = {
      write(testData)
    }
  }

  def decodeRunner[A](data:A)(implicit p:Writer[A], u:Reader[A]):PushkaRunner[A] = new PushkaRunner[A](data) {
    var testData : A = _
    var s:String = _

    override def initialize = {
      testData = data
      s = write(testData)
      s.getBytes(StandardCharsets.UTF_8)
    }

    override def run = {
      read[A](s)
    }
  }
}
