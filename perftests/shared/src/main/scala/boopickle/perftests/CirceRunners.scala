package boopickle.perftests

import java.nio.charset.StandardCharsets

import io.circe._
import io.circe.syntax._

abstract class CirceRunner[A](data: A) extends TestRunner[A](data) {
  override def name = "Circe"
}

object CirceRunners {

  def encodeRunner[A](data: A)(implicit p: Encoder[A], u: Decoder[A]): CirceRunner[A] = new CirceRunner[A](data) {
    var testData: A = _

    override def initialize = {
      testData = data
      val res = testData.asJson.noSpaces
      // println(res)
      res.getBytes(StandardCharsets.UTF_8)
    }

    override def run = {
      testData.asJson.noSpaces
    }
  }

  def decodeRunner[A](data: A)(implicit p: Encoder[A], u: Decoder[A]): CirceRunner[A] = new CirceRunner[A](data) {
    var testData: A = _
    var s: String = _

    override def initialize = {
      testData = data
      s = testData.asJson.noSpaces
      s.getBytes(StandardCharsets.UTF_8)
    }

    override def run = {
      u.decodeJson(parse.parse(s).toOption.get)
    }
  }
}
