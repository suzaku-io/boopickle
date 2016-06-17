package boopickle.perftests

import boopickle.Default._
import pushka.json._
import io.circe.generic.auto._

object Tests {
  val tree = TestData.genTree(5, 3)
  val suites = Seq(
    PerfTestSuite("Encode single Event(42)", Seq(
      BooPickleRunners.encodeRunner(Event(42)),
      BooPickleSpeedRunners.encodeRunner(Event(42)),
      PrickleRunners.encodeRunner(Event(42)),
      UPickleRunners.encodeRunner(Event(42)),
      CirceRunners.encodeRunner(Event(42)),
      PushkaRunners.encodeRunner(Event(42))
    ))
    ,
    PerfTestSuite("Decode single Event(42)", Seq(
      BooPickleRunners.decodeRunner(Event(42)),
      BooPickleSpeedRunners.decodeRunner(Event(42)),
      PrickleRunners.decodeRunner(Event(42)),
      UPickleRunners.decodeRunner(Event(42)),
      CirceRunners.decodeRunner(Event(42)),
      PushkaRunners.decodeRunner(Event(42))
    )),
    PerfTestSuite("Encode very large Array[Int]", Seq(
      BooPickleRunners.encodeRunner(TestData.largeIntSeq),
      BooPickleSpeedRunners.encodeRunner(TestData.largeIntSeq),
      // PrickleRunners.encodeRunner(TestData.largeIntSeq),
      UPickleRunners.encodeRunner(TestData.largeIntSeq),
      CirceRunners.encodeRunner(TestData.largeIntSeq)
      // PushkaRunners.encodeRunner(TestData.largeIntSeq)
    )),
    PerfTestSuite("Decode very large Array[Int]", Seq(
      BooPickleRunners.decodeRunner(TestData.largeIntSeq),
      BooPickleSpeedRunners.decodeRunner(TestData.largeIntSeq),
      // PrickleRunners.decodeRunner(TestData.largeIntSeq),
      UPickleRunners.decodeRunner(TestData.largeIntSeq),
      CirceRunners.decodeRunner(TestData.largeIntSeq)
      // PushkaRunners.decodeRunner(TestData.largeIntSeq)
    )),
    PerfTestSuite("Encode large Array[Double]", Seq(
      BooPickleRunners.encodeRunner(TestData.largeDoubleSeq),
      BooPickleSpeedRunners.encodeRunner(TestData.largeDoubleSeq),
      //PrickleRunners.encodeRunner(TestData.largeDoubleSeq),
      UPickleRunners.encodeRunner(TestData.largeDoubleSeq),
      CirceRunners.encodeRunner(TestData.largeDoubleSeq)
      //PushkaRunners.encodeRunner(TestData.largeDoubleSeq)
    )),
    PerfTestSuite("Decode large Array[Double]", Seq(
      BooPickleRunners.decodeRunner(TestData.largeDoubleSeq),
      BooPickleSpeedRunners.decodeRunner(TestData.largeDoubleSeq),
      //PrickleRunners.decodeRunner(TestData.largeDoubleSeq),
      UPickleRunners.decodeRunner(TestData.largeDoubleSeq),
      CirceRunners.decodeRunner(TestData.largeDoubleSeq)
      //PushkaRunners.decodeRunner(TestData.largeDoubleSeq)
    )),
    PerfTestSuite("Encode large Array[Float]", Seq(
      BooPickleRunners.encodeRunner(TestData.largeFloatSeq),
      BooPickleSpeedRunners.encodeRunner(TestData.largeFloatSeq),
      //PrickleRunners.encodeRunner(TestData.largeFloatSeq),
      UPickleRunners.encodeRunner(TestData.largeFloatSeq),
      CirceRunners.encodeRunner(TestData.largeFloatSeq)
      //PushkaRunners.encodeRunner(TestData.largeFloatSeq)
    )),
    PerfTestSuite("Decode large Array[Float]", Seq(
      BooPickleRunners.decodeRunner(TestData.largeFloatSeq),
      BooPickleSpeedRunners.decodeRunner(TestData.largeFloatSeq),
      //PrickleRunners.decodeRunner(TestData.largeFloatSeq),
      UPickleRunners.decodeRunner(TestData.largeFloatSeq),
      CirceRunners.decodeRunner(TestData.largeFloatSeq)
      //PushkaRunners.decodeRunner(TestData.largeFloatSeq)
    )),
    PerfTestSuite("Encode an object tree", Seq(
      BooPickleRunners.encodeRunner(tree),
      BooPickleSpeedRunners.encodeRunner(tree),
      PrickleRunners.encodeRunner(tree),
      UPickleRunners.encodeRunner(tree),
      CirceRunners.encodeRunner(tree),
      PushkaRunners.encodeRunner(tree)
    )),
    PerfTestSuite("Decode an object tree", Seq(
      BooPickleRunners.decodeRunner(tree),
      BooPickleSpeedRunners.decodeRunner(tree),
      PrickleRunners.decodeRunner(tree),
      UPickleRunners.decodeRunner(tree),
      CirceRunners.decodeRunner(tree),
      PushkaRunners.decodeRunner(tree)
    )),
    PerfTestSuite("Encode very large Map[String, Int]", List(
      BooPickleRunners.encodeRunner(TestData.largeStringIntMap),
      BooPickleSpeedRunners.encodeRunner(TestData.largeStringIntMap),
      PrickleRunners.encodeRunner(TestData.largeStringIntMap),
      UPickleRunners.encodeRunner(TestData.largeStringIntMap),
      CirceRunners.encodeRunner(TestData.largeStringIntMap),
      PushkaRunners.encodeRunner(TestData.largeStringIntMap)
    )),
    PerfTestSuite("Decode very large Map[String, Int]", List(
      BooPickleRunners.decodeRunner(TestData.largeStringIntMap),
      BooPickleSpeedRunners.decodeRunner(TestData.largeStringIntMap),
      PrickleRunners.decodeRunner(TestData.largeStringIntMap),
      UPickleRunners.decodeRunner(TestData.largeStringIntMap),
      CirceRunners.decodeRunner(TestData.largeStringIntMap),
      PushkaRunners.decodeRunner(TestData.largeStringIntMap)
    ))
    ,
    PerfTestSuite("Encoding Seq[Book] with random IDs", Seq(
      BooPickleRunners.encodeRunner(TestData.booksRandomID),
      BooPickleSpeedRunners.encodeRunner(TestData.booksRandomID),
      PrickleRunners.encodeRunner(TestData.booksRandomID),
      UPickleRunners.encodeRunner(TestData.booksRandomID),
      CirceRunners.encodeRunner(TestData.booksRandomID),
      PushkaRunners.encodeRunner(TestData.booksRandomID)
    )),
    PerfTestSuite("Decoding Seq[Book] with random IDs", Seq(
      BooPickleRunners.decodeRunner(TestData.booksRandomID),
      BooPickleSpeedRunners.decodeRunner(TestData.booksRandomID),
      PrickleRunners.decodeRunner(TestData.booksRandomID),
      UPickleRunners.decodeRunner(TestData.booksRandomID),
      CirceRunners.decodeRunner(TestData.booksRandomID),
      PushkaRunners.decodeRunner(TestData.booksRandomID)
    )),
    PerfTestSuite("Encoding Seq[Book] with numerical IDs", Seq(
      BooPickleRunners.encodeRunner(TestData.booksNumId),
      BooPickleSpeedRunners.encodeRunner(TestData.booksNumId),
      PrickleRunners.encodeRunner(TestData.booksNumId),
      UPickleRunners.encodeRunner(TestData.booksNumId),
      CirceRunners.encodeRunner(TestData.booksNumId),
      PushkaRunners.encodeRunner(TestData.booksNumId)
    )),
    PerfTestSuite("Decoding Seq[Book] with numerical IDs", Seq(
      BooPickleRunners.decodeRunner(TestData.booksNumId),
      BooPickleSpeedRunners.decodeRunner(TestData.booksNumId),
      PrickleRunners.decodeRunner(TestData.booksNumId),
      UPickleRunners.decodeRunner(TestData.booksNumId),
      CirceRunners.decodeRunner(TestData.booksNumId),
      PushkaRunners.decodeRunner(TestData.booksNumId)
    ))
  )
}
