package boopickle.perftests

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class CirceBenchmarks extends TestData with CirceCoding

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class BoopickleBenchmarks extends TestData with BoopickleCoding

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class BoopickleSpeedBenchmarks extends TestData with BoopickleSpeedCoding
