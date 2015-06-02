import sbt._
import Keys._
import com.typesafe.sbt.pgp.PgpKeys._

val commonSettings = Seq(
  organization := "me.chrons",
  version := Version.library,
  scalaVersion := "2.11.6",
  scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8"),
  testFrameworks += new TestFramework("utest.runner.Framework"),
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "utest" % "0.3.1" % "test",
    "com.github.japgolly.nyaya" %%% "nyaya-test" % "0.5.11" % "test",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
  )
)

def preventPublication(p: Project) =
  p.settings(
    publish            := (),
    publishLocal       := (),
    publishSigned      := (),
    publishLocalSigned := (),
    publishArtifact    := false,
    publishTo          := Some(Resolver.file("Unused transient repository", target.value / "fakepublish")),
    packagedArtifacts  := Map.empty)

lazy val boopickle = crossProject
  .settings(commonSettings: _*)
  .settings(
    name := "boopickle",
    scmInfo := Some(ScmInfo(
      url("https://github.com/ochrons/boopickle"),
      "scm:git:git@github.com:ochrons/boopickle.git",
      Some("scm:git:git@github.com:ochrons/boopickle.git"))),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomExtra :=
      <url>https://github.com/ochrons/boopickle</url>
      <licenses>
        <license>
          <name>MIT license</name>
          <url>http://www.opensource.org/licenses/mit-license.php</url>
        </license>
      </licenses>
        <developers>
          <developer>
            <id>ochrons</id>
            <name>Otto Chrons</name>
            <url>https://github.com/ochrons</url>
          </developer>
        </developers>,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  ).jsSettings(
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

/*
Generator for all the 22 Tuple picklers. Resulting source files are written under `target`
from where they need to be copied to the `boopickle` source directory.
 */
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

lazy val perftests = crossProject
  .settings(commonSettings: _*)
  .settings(
    name := "perftests",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % "0.2.8",
      "com.github.benhutchison" %%% "prickle" % "1.1.6"
    )
  )
  .jsSettings(
    bootSnippet := "BooApp().main();",
    libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.0",
    "com.lihaoyi" %%% "scalatags" % "0.4.6"
    )
  )

lazy val perftestsJS = preventPublication(perftests.js).settings(workbenchSettings: _*).dependsOn(boopickleJS)

lazy val perftestsJVM = preventPublication(perftests.jvm).dependsOn(boopickleJVM)


lazy val root = preventPublication(project.in(file(".")))
  .settings()
  .aggregate(boopickleJS, boopickleJVM, perftestsJS, perftestsJVM)
