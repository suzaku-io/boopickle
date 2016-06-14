package boopickle.perftests

case class PerfTestSuite(name: String, runners: Seq[TestRunner[_]])

abstract class TestRunner[A](val data: A) {
  /**
   * Name of the runner
   */
  def name: String

  /**
   * Initialize runner, for example by encoding input data
   *
   * @return Encoded bytes
   */
  def initialize: Array[Byte]

  /**
   * Run the test case
   */
  def run(): Unit
}

case class PerfTestGroupResult(name: String, results: Seq[PerfTestResult])

case class PerfTestResult(name: String, count: Int, data: Array[Byte])

class PerfTester(suite: PerfTestSuite) {
  val testTime = 500

  def runSuite: PerfTestGroupResult = {
    // initialize all runners
    val datas = suite.runners.map { runner =>
      runner.initialize
    }
    val startTime = System.currentTimeMillis()
    // warm up the VM
    while (System.currentTimeMillis() - startTime < testTime / 2) {
      suite.runners.foreach(_.run)
    }
    // analyze performance
    val counters = suite.runners.map { runner =>
      val startTime = System.currentTimeMillis()
      var counter = 0
      while (System.currentTimeMillis() - startTime < testTime) {
        runner.run
        counter += 1
      }
      val endTime = System.currentTimeMillis()
      (counter * 1000L / (endTime - startTime)).toInt
    }
    val results = (suite.runners.map(_.name), counters, datas).zipped.toList.map((PerfTestResult.apply _).tupled)
    PerfTestGroupResult(suite.name, results)
  }
}
