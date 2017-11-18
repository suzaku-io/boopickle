package boopickle.perftests

import boopickle.Default._
import io.circe.generic.auto._

object Tests {
  val tree = TestData.genTree(5, 3)
  val suites = Seq(
    PerfTestSuite(
      "Encode single Event(42, true)",
      Seq(
        BooPickleRunners.encodeRunner(Event(42, true)),
        BooPickleSpeedRunners.encodeRunner(Event(42, true)),
        PrickleRunners.encodeRunner(Event(42, true)),
        UPickleRunners.encodeRunner(Event(42, true)),
        CirceRunners.encodeRunner(Event(42, true)),
      )
    ),
    PerfTestSuite(
      "Decode single Event(42, true)",
      Seq(
        BooPickleRunners.decodeRunner(Event(42, true)),
        BooPickleSpeedRunners.decodeRunner(Event(42, true)),
        PrickleRunners.decodeRunner(Event(42, true)),
        UPickleRunners.decodeRunner(Event(42, true)),
        CirceRunners.decodeRunner(Event(42, true)),
      )
    ),
    PerfTestSuite(
      "Encode very large Array[Int]",
      Seq(
        BooPickleRunners.encodeRunner(TestData.largeIntSeq),
        BooPickleSpeedRunners.encodeRunner(TestData.largeIntSeq),
        // PrickleRunners.encodeRunner(TestData.largeIntSeq),
        UPickleRunners.encodeRunner(TestData.largeIntSeq),
        CirceRunners.encodeRunner(TestData.largeIntSeq)
      )
    ),
    PerfTestSuite(
      "Decode very large Array[Int]",
      Seq(
        BooPickleRunners.decodeRunner(TestData.largeIntSeq),
        BooPickleSpeedRunners.decodeRunner(TestData.largeIntSeq),
        // PrickleRunners.decodeRunner(TestData.largeIntSeq),
        UPickleRunners.decodeRunner(TestData.largeIntSeq),
        CirceRunners.decodeRunner(TestData.largeIntSeq)
      )
    ),
    PerfTestSuite(
      "Encode large Array[Double]",
      Seq(
        BooPickleRunners.encodeRunner(TestData.largeDoubleSeq),
        BooPickleSpeedRunners.encodeRunner(TestData.largeDoubleSeq),
        //PrickleRunners.encodeRunner(TestData.largeDoubleSeq),
        UPickleRunners.encodeRunner(TestData.largeDoubleSeq),
        CirceRunners.encodeRunner(TestData.largeDoubleSeq)
      )
    ),
    PerfTestSuite(
      "Decode large Array[Double]",
      Seq(
        BooPickleRunners.decodeRunner(TestData.largeDoubleSeq),
        BooPickleSpeedRunners.decodeRunner(TestData.largeDoubleSeq),
        //PrickleRunners.decodeRunner(TestData.largeDoubleSeq),
        UPickleRunners.decodeRunner(TestData.largeDoubleSeq),
        CirceRunners.decodeRunner(TestData.largeDoubleSeq)
      )
    ),
    PerfTestSuite(
      "Encode large Seq[String]",
      Seq(
        BooPickleRunners.encodeRunner(TestData.largeStringSeq),
        BooPickleSpeedRunners.encodeRunner(TestData.largeStringSeq),
        PrickleRunners.encodeRunner(TestData.largeStringSeq),
        UPickleRunners.encodeRunner(TestData.largeStringSeq),
        CirceRunners.encodeRunner(TestData.largeStringSeq),
      )
    ),
    PerfTestSuite(
      "Decode large Seq[String]",
      Seq(
        BooPickleRunners.decodeRunner(TestData.largeStringSeq),
        BooPickleSpeedRunners.decodeRunner(TestData.largeStringSeq),
        PrickleRunners.decodeRunner(TestData.largeStringSeq),
        UPickleRunners.decodeRunner(TestData.largeStringSeq),
        CirceRunners.decodeRunner(TestData.largeStringSeq),
      )
    ),
    PerfTestSuite(
      "Encode an object tree",
      Seq(
        BooPickleRunners.encodeRunner(tree),
        BooPickleSpeedRunners.encodeRunner(tree),
        PrickleRunners.encodeRunner(tree),
        UPickleRunners.encodeRunner(tree),
        CirceRunners.encodeRunner(tree),
      )
    ),
    PerfTestSuite(
      "Decode an object tree",
      Seq(
        BooPickleRunners.decodeRunner(tree),
        BooPickleSpeedRunners.decodeRunner(tree),
        PrickleRunners.decodeRunner(tree),
        UPickleRunners.decodeRunner(tree),
        CirceRunners.decodeRunner(tree),
      )
    ),
    PerfTestSuite(
      "Encode very large Map[String, Int]",
      List(
        BooPickleRunners.encodeRunner(TestData.largeStringIntMap),
        BooPickleSpeedRunners.encodeRunner(TestData.largeStringIntMap),
        PrickleRunners.encodeRunner(TestData.largeStringIntMap),
        UPickleRunners.encodeRunner(TestData.largeStringIntMap),
        CirceRunners.encodeRunner(TestData.largeStringIntMap),
      )
    ),
    PerfTestSuite(
      "Decode very large Map[String, Int]",
      List(
        BooPickleRunners.decodeRunner(TestData.largeStringIntMap),
        BooPickleSpeedRunners.decodeRunner(TestData.largeStringIntMap),
        PrickleRunners.decodeRunner(TestData.largeStringIntMap),
        UPickleRunners.decodeRunner(TestData.largeStringIntMap),
        CirceRunners.decodeRunner(TestData.largeStringIntMap),
      )
    ),
    PerfTestSuite(
      "Encoding Seq[Book] with random IDs",
      Seq(
        BooPickleRunners.encodeRunner(TestData.booksRandomID),
        BooPickleSpeedRunners.encodeRunner(TestData.booksRandomID),
        PrickleRunners.encodeRunner(TestData.booksRandomID),
        UPickleRunners.encodeRunner(TestData.booksRandomID),
        CirceRunners.encodeRunner(TestData.booksRandomID),
      )
    ),
    PerfTestSuite(
      "Decoding Seq[Book] with random IDs",
      Seq(
        BooPickleRunners.decodeRunner(TestData.booksRandomID),
        BooPickleSpeedRunners.decodeRunner(TestData.booksRandomID),
        PrickleRunners.decodeRunner(TestData.booksRandomID),
        UPickleRunners.decodeRunner(TestData.booksRandomID),
        CirceRunners.decodeRunner(TestData.booksRandomID),
      )
    ),
    PerfTestSuite(
      "Encoding Seq[Book] with numerical IDs",
      Seq(
        BooPickleRunners.encodeRunner(TestData.booksNumId),
        BooPickleSpeedRunners.encodeRunner(TestData.booksNumId),
        PrickleRunners.encodeRunner(TestData.booksNumId),
        UPickleRunners.encodeRunner(TestData.booksNumId),
        CirceRunners.encodeRunner(TestData.booksNumId),
      )
    ),
    PerfTestSuite(
      "Decoding Seq[Book] with numerical IDs",
      Seq(
        BooPickleRunners.decodeRunner(TestData.booksNumId),
        BooPickleSpeedRunners.decodeRunner(TestData.booksNumId),
        PrickleRunners.decodeRunner(TestData.booksNumId),
        UPickleRunners.decodeRunner(TestData.booksNumId),
        CirceRunners.decodeRunner(TestData.booksNumId),
      )
    )
  )
}
