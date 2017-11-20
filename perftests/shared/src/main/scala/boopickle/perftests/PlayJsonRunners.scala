package boopickle.perftests

import java.nio.charset.StandardCharsets

import play.api.libs.json._

abstract class PlayJsonRunner[A](data: A) extends TestRunner[A](data) {
  override def name = "Play JSON"
}

object PlayJsonRunners {
  def encodeRunner[A](data: A)(implicit p: Format[A]): PlayJsonRunner[A] = new PlayJsonRunner[A](data) {
    var testData: A = _

    override def initialize = {
      testData = data
      val res = Json.stringify(Json.toJson(testData))
      // println(res)
      res.getBytes(StandardCharsets.UTF_8)
    }

    override def run(): Any = {
      Json.stringify(Json.toJson(testData))
    }
  }

  def decodeRunner[A](data: A)(implicit p: Format[A]): PlayJsonRunner[A] = new PlayJsonRunner[A](data) {
    var testData: A = _
    var s: String   = _

    override def initialize = {
      testData = data
      s = Json.stringify(Json.toJson(testData))
      s.getBytes(StandardCharsets.UTF_8)
    }

    override def run(): Any = {
      p.reads(Json.parse(s))
    }
  }
}
