import sbt.Keys._

val boopickle = crossProject.settings(
  organization := "me.chrons",
  version := "0.0.1",
  scalaVersion := "2.11.6",
  name := "boopickle",
  scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8"),
  testFrameworks += new TestFramework("utest.runner.Framework"),
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "utest" % "0.3.1" % "test",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
  )).jsSettings(
    // use PhantomJS for testing, because we need real browser JS stuff like TypedArrays
    scalaJSStage in Global := FastOptStage,
    jsDependencies += RuntimeDOM,
    scalacOptions ++= (if (isSnapshot.value) Seq.empty
    else Seq({
      val a = baseDirectory.value.toURI.toString.replaceFirst("[^/]+/?$", "")
      val g = "https://raw.githubusercontent.com/ochrons/boopickle"
      s"-P:scalajs:mapSourceURI:$a->$g/v${version.value}/"
    }))
  ).jvmSettings(
  )

lazy val boopickleJS = boopickle.js

lazy val boopickleJVM = boopickle.jvm

lazy val generateTuples = taskKey[Unit]("Generates source code for pickling tuples")

generateTuples := {
  val (picklers, unpicklers) = (1 to 22).map { i =>
    def commaSeparated(s: Int => String, sep: String = ", ") = (1 to i).map(s).mkString(sep)
    val picklerTypes = commaSeparated(j => s"T$j: P")
    val unpicklerTypes = commaSeparated(j => s"T$j: U")
    val typeTuple = if (i == 1) s"Tuple1[T1]" else s"(${commaSeparated(j => s"T$j")})"
    val writes = commaSeparated(j => s"write[T$j](x._$j)", "; ")
    val reads = commaSeparated(j => s"read[T$j]")

    ( s"""
  implicit def Tuple${i}Pickler[$picklerTypes] = new P[$typeTuple] {
    override def pickle(x: $typeTuple)(implicit state: PickleState): Unit = { $writes }
  }""",
      s"""
  implicit def Tuple${i}Unpickler[$unpicklerTypes] = new U[$typeTuple] {
    override def unpickle(implicit state: UnpickleState) = ${if (i == 1) s"Tuple1[T1]" else ""}($reads)
  }""")
  }.unzip
  IO.write(baseDirectory.value / "target" / "TuplePicklers.scala",
    s"""package boopickle

trait TuplePicklers extends PicklerHelper {
  ${picklers.mkString("\n")}
}
""")
  IO.write(baseDirectory.value / "target" / "TupleUnpicklers.scala",
    s"""package boopickle

trait TupleUnpicklers extends UnpicklerHelper {
  ${unpicklers.mkString("\n")}
}
""")
}

