package boopickle.perftests

object Tests {
  val tree = TestData.genTree(5, 3)
  val suites = Seq(
    PerfTestSuite("Encode single Seq[Int]", Seq(
      BooPickleRunners.encodeRunner(Seq(3)),
      PrickleRunners.encodeRunner(Seq(3)),
      UPickleRunners.encodeRunner(Seq(3))
    )),
    PerfTestSuite("Decode single Seq[Int]", Seq(
      BooPickleRunners.decodeRunner(Seq(3)),
      PrickleRunners.decodeRunner(Seq(3)),
      UPickleRunners.decodeRunner(Seq(3))
    )),
    PerfTestSuite("Encode very large Seq[Int]", Seq(
      BooPickleRunners.encodeRunner(TestData.largeIntSeq),
      PrickleRunners.encodeRunner(TestData.largeIntSeq),
      UPickleRunners.encodeRunner(TestData.largeIntSeq)
    )),
    PerfTestSuite("Decode very large Seq[Int]", Seq(
      BooPickleRunners.decodeRunner(TestData.largeIntSeq),
      PrickleRunners.decodeRunner(TestData.largeIntSeq),
      UPickleRunners.decodeRunner(TestData.largeIntSeq)
    )),
    PerfTestSuite("Encode large Seq[Double]", Seq(
      BooPickleRunners.encodeRunner(TestData.largeDoubleSeq),
      PrickleRunners.encodeRunner(TestData.largeDoubleSeq),
      UPickleRunners.encodeRunner(TestData.largeDoubleSeq)
    )),
    PerfTestSuite("Decode large Seq[Double]", Seq(
      BooPickleRunners.decodeRunner(TestData.largeDoubleSeq),
      PrickleRunners.decodeRunner(TestData.largeDoubleSeq),
      UPickleRunners.decodeRunner(TestData.largeDoubleSeq)
    )),
    PerfTestSuite("Encode large Seq[Float]", Seq(
      BooPickleRunners.encodeRunner(TestData.largeFloatSeq),
      PrickleRunners.encodeRunner(TestData.largeFloatSeq),
      UPickleRunners.encodeRunner(TestData.largeFloatSeq)
    )),
    PerfTestSuite("Decode large Seq[Float]", Seq(
      BooPickleRunners.decodeRunner(TestData.largeFloatSeq),
      PrickleRunners.decodeRunner(TestData.largeFloatSeq),
      UPickleRunners.decodeRunner(TestData.largeFloatSeq)
    )),
    PerfTestSuite("Encode a tree", Seq(
      BooPickleRunners.encodeRunner(tree),
      PrickleRunners.encodeRunner(tree),
      UPickleRunners.encodeRunner(tree)
    )),
    PerfTestSuite("Decode a tree", Seq(
      BooPickleRunners.decodeRunner(tree),
      PrickleRunners.decodeRunner(tree),
      UPickleRunners.decodeRunner(tree)
    )),
    PerfTestSuite("Encoding Seq[Book] with random IDs", Seq(
      BooPickleRunners.encodeRunner(TestData.booksRandomID),
      PrickleRunners.encodeRunner(TestData.booksRandomID),
      UPickleRunners.encodeRunner(TestData.booksRandomID)
    )),
    PerfTestSuite("Decoding Seq[Book] with random IDs", Seq(
      BooPickleRunners.decodeRunner(TestData.booksRandomID),
      PrickleRunners.decodeRunner(TestData.booksRandomID),
      UPickleRunners.decodeRunner(TestData.booksRandomID)
    )),
    PerfTestSuite("Encoding Seq[Book] with UUIDs", Seq(
      BooPickleRunners.encodeRunner(TestData.booksUUID),
      PrickleRunners.encodeRunner(TestData.booksUUID),
      UPickleRunners.encodeRunner(TestData.booksUUID)
    )),
    PerfTestSuite("Decoding Seq[Book] with UUIDs", Seq(
      BooPickleRunners.decodeRunner(TestData.booksUUID),
      PrickleRunners.decodeRunner(TestData.booksUUID),
      UPickleRunners.decodeRunner(TestData.booksUUID)
    )),
    PerfTestSuite("Encoding Seq[Book] with numerical IDs", Seq(
      BooPickleRunners.encodeRunner(TestData.booksNumId),
      PrickleRunners.encodeRunner(TestData.booksNumId),
      UPickleRunners.encodeRunner(TestData.booksNumId)
    )),
    PerfTestSuite("Decoding Seq[Book] with numerical IDs", Seq(
      BooPickleRunners.decodeRunner(TestData.booksNumId),
      PrickleRunners.decodeRunner(TestData.booksNumId),
      UPickleRunners.decodeRunner(TestData.booksNumId)
    ))
  )
}
