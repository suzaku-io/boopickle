package boopickle.perftests

import boopickle.Default._
import io.circe.generic.auto._
import play.api.libs.json._
import upickle.default._

object Tests {
  implicit val eventFormat: OFormat[Event]     = Json.format[Event]
  implicit val bookFormat: OFormat[Book]       = Json.format[Book]
  implicit val nodeFormat: OFormat[Node]       = Json.format[Node]
  implicit val eventUpickle: ReadWriter[Event] = macroRW
  implicit val eventNode: ReadWriter[Node]     = macroRW
  implicit val eventBook: ReadWriter[Book]     = macroRW

  val tree = TestData.genTree(5, 3)
  val suites = Seq(
    PerfTestSuite(
      "Encode single Event(42, true)",
      Seq(
        BooPickleRunners.encodeRunner(Event(42, true)),
        BooPickleSpeedRunners.encodeRunner(Event(42, true)),
        UPickleRunners.encodeRunner(Event(42, true)),
        CirceRunners.encodeRunner(Event(42, true)),
        PlayJsonRunners.encodeRunner(Event(42, true))
      )
    ),
    PerfTestSuite(
      "Decode single Event(42, true)",
      Seq(
        BooPickleRunners.decodeRunner(Event(42, true)),
        BooPickleSpeedRunners.decodeRunner(Event(42, true)),
        UPickleRunners.decodeRunner(Event(42, true)),
        CirceRunners.decodeRunner(Event(42, true)),
        PlayJsonRunners.decodeRunner(Event(42, true))
      )
    ),
    PerfTestSuite(
      "Encode very large Array[Int]",
      Seq(
        BooPickleRunners.encodeRunner(TestData.largeIntSeq),
        BooPickleSpeedRunners.encodeRunner(TestData.largeIntSeq),
        UPickleRunners.encodeRunner(TestData.largeIntSeq),
        CirceRunners.encodeRunner(TestData.largeIntSeq),
        PlayJsonRunners.encodeRunner(TestData.largeIntSeq)
      )
    ),
    PerfTestSuite(
      "Decode very large Array[Int]",
      Seq(
        BooPickleRunners.decodeRunner(TestData.largeIntSeq),
        BooPickleSpeedRunners.decodeRunner(TestData.largeIntSeq),
        UPickleRunners.decodeRunner(TestData.largeIntSeq),
        CirceRunners.decodeRunner(TestData.largeIntSeq),
        PlayJsonRunners.decodeRunner(TestData.largeIntSeq)
      )
    ),
    PerfTestSuite(
      "Encode large Array[Double]",
      Seq(
        BooPickleRunners.encodeRunner(TestData.largeDoubleSeq),
        BooPickleSpeedRunners.encodeRunner(TestData.largeDoubleSeq),
        UPickleRunners.encodeRunner(TestData.largeDoubleSeq),
        CirceRunners.encodeRunner(TestData.largeDoubleSeq),
        PlayJsonRunners.encodeRunner(TestData.largeDoubleSeq)
      )
    ),
    PerfTestSuite(
      "Decode large Array[Double]",
      Seq(
        BooPickleRunners.decodeRunner(TestData.largeDoubleSeq),
        BooPickleSpeedRunners.decodeRunner(TestData.largeDoubleSeq),
        UPickleRunners.decodeRunner(TestData.largeDoubleSeq),
        CirceRunners.decodeRunner(TestData.largeDoubleSeq),
        PlayJsonRunners.decodeRunner(TestData.largeDoubleSeq)
      )
    ),
    PerfTestSuite(
      "Encode large Seq[String]",
      Seq(
        BooPickleRunners.encodeRunner(TestData.largeStringSeq),
        BooPickleSpeedRunners.encodeRunner(TestData.largeStringSeq),
        UPickleRunners.encodeRunner(TestData.largeStringSeq),
        CirceRunners.encodeRunner(TestData.largeStringSeq),
        PlayJsonRunners.encodeRunner(TestData.largeStringSeq)
      )
    ),
    PerfTestSuite(
      "Decode large Seq[String]",
      Seq(
        BooPickleRunners.decodeRunner(TestData.largeStringSeq),
        BooPickleSpeedRunners.decodeRunner(TestData.largeStringSeq),
        UPickleRunners.decodeRunner(TestData.largeStringSeq),
        CirceRunners.decodeRunner(TestData.largeStringSeq),
        PlayJsonRunners.decodeRunner(TestData.largeStringSeq)
      )
    ),
    PerfTestSuite(
      "Encode an object tree",
      Seq(
        BooPickleRunners.encodeRunner(tree),
        BooPickleSpeedRunners.encodeRunner(tree),
        UPickleRunners.encodeRunner(tree),
        CirceRunners.encodeRunner(tree),
        PlayJsonRunners.encodeRunner(tree)
      )
    ),
    PerfTestSuite(
      "Decode an object tree",
      Seq(
        BooPickleRunners.decodeRunner(tree),
        BooPickleSpeedRunners.decodeRunner(tree),
        UPickleRunners.decodeRunner(tree),
        CirceRunners.decodeRunner(tree),
        PlayJsonRunners.decodeRunner(tree)
      )
    ),
    PerfTestSuite(
      "Encode very large Map[String, Int]",
      List(
        BooPickleRunners.encodeRunner(TestData.largeStringIntMap),
        BooPickleSpeedRunners.encodeRunner(TestData.largeStringIntMap),
        UPickleRunners.encodeRunner(TestData.largeStringIntMap),
        CirceRunners.encodeRunner(TestData.largeStringIntMap),
        PlayJsonRunners.encodeRunner(TestData.largeStringIntMap)
      )
    ),
    PerfTestSuite(
      "Decode very large Map[String, Int]",
      List(
        BooPickleRunners.decodeRunner(TestData.largeStringIntMap),
        BooPickleSpeedRunners.decodeRunner(TestData.largeStringIntMap),
        UPickleRunners.decodeRunner(TestData.largeStringIntMap),
        CirceRunners.decodeRunner(TestData.largeStringIntMap),
        PlayJsonRunners.decodeRunner(TestData.largeStringIntMap)
      )
    ),
    PerfTestSuite(
      "Encoding Seq[Book] with random IDs",
      Seq(
        BooPickleRunners.encodeRunner(TestData.booksRandomID),
        BooPickleSpeedRunners.encodeRunner(TestData.booksRandomID),
        UPickleRunners.encodeRunner(TestData.booksRandomID),
        CirceRunners.encodeRunner(TestData.booksRandomID),
        PlayJsonRunners.encodeRunner(TestData.booksRandomID)
      )
    ),
    PerfTestSuite(
      "Decoding Seq[Book] with random IDs",
      Seq(
        BooPickleRunners.decodeRunner(TestData.booksRandomID),
        BooPickleSpeedRunners.decodeRunner(TestData.booksRandomID),
        UPickleRunners.decodeRunner(TestData.booksRandomID),
        CirceRunners.decodeRunner(TestData.booksRandomID),
        PlayJsonRunners.decodeRunner(TestData.booksRandomID)
      )
    ),
    PerfTestSuite(
      "Encoding Seq[Book] with numerical IDs",
      Seq(
        BooPickleRunners.encodeRunner(TestData.booksNumId),
        BooPickleSpeedRunners.encodeRunner(TestData.booksNumId),
        UPickleRunners.encodeRunner(TestData.booksNumId),
        CirceRunners.encodeRunner(TestData.booksNumId),
        PlayJsonRunners.encodeRunner(TestData.booksNumId)
      )
    ),
    PerfTestSuite(
      "Decoding Seq[Book] with numerical IDs",
      Seq(
        BooPickleRunners.decodeRunner(TestData.booksNumId),
        BooPickleSpeedRunners.decodeRunner(TestData.booksNumId),
        UPickleRunners.decodeRunner(TestData.booksNumId),
        CirceRunners.decodeRunner(TestData.booksNumId),
        PlayJsonRunners.decodeRunner(TestData.booksNumId)
      )
    )
  )
}
