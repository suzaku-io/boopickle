package boopickle.perftests

import java.nio.charset.StandardCharsets

import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.semiauto._
import org.openjdk.jmh.annotations.Benchmark

abstract class CirceRunner[A](data: A) extends TestRunner[A](data) {
  override def name = "Circe"
}

object CirceRunners {

  def encodeRunner[A](data: A)(implicit p: Encoder[A], u: Decoder[A]): CirceRunner[A] = new CirceRunner[A](data) {
    var testData: A = _

    override def initialize = {
      testData = data
      val res = testData.asJson.noSpaces
      res.getBytes(StandardCharsets.UTF_8)
    }

    override def run(): Any = {
      testData.asJson.noSpaces
      ()
    }
  }

  def decodeRunner[A](data: A)(implicit p: Encoder[A], u: Decoder[A]): CirceRunner[A] = new CirceRunner[A](data) {
    var testData: A = _
    var s: String   = _

    override def initialize = {
      testData = data
      s = testData.asJson.noSpaces
      s.getBytes(StandardCharsets.UTF_8)
    }

    override def run(): Any = {
      u.decodeJson(parser.parse(s).toOption.get)
      ()
    }
  }
}

trait CirceCoding { self: TestData =>
  private implicit val eventEncoder: Encoder[Event] = deriveEncoder[Event]
  private lazy val eventDecoder                     = deriveDecoder[Event]

  private lazy val eventJson   = event.asJson.noSpaces
  private lazy val intsJson    = largeIntSeq.asJson.noSpaces
  private lazy val doublesJson = largeDoubleSeq.asJson.noSpaces

  @Benchmark
  def circeEventDecode: Event = {
    eventDecoder.decodeJson(parse(eventJson).right.get).right.get
  }

  @Benchmark
  def circeEventEncode: String = {
    eventEncoder(event).noSpaces
  }

  @Benchmark
  def circeIntArrayDecode: Array[Int] = {
    decode[Array[Int]](intsJson).right.get
  }

  @Benchmark
  def circeIntArrayEncode: String = {
    largeIntSeq.asJson.noSpaces
  }

  @Benchmark
  def circeDoubleArrayDecode: Array[Double] = {
    decode[Array[Double]](doublesJson).right.get
  }

  @Benchmark
  def circeDoubleArrayEncode: String = {
    largeDoubleSeq.asJson.noSpaces
  }
}
