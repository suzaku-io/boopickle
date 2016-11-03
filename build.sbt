import sbt._
import Keys._
import com.typesafe.sbt.pgp.PgpKeys._
import org.scalajs.sbtplugin.OptimizerOptions

val commonSettings = Seq(
  organization := "me.chrons",
  version := Version.library,
  scalaVersion := "2.11.8",
  scalacOptions := Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"),
  scalacOptions in Compile ~= (_ filterNot (_ == "-Ywarn-value-discard")),
  testFrameworks += new TestFramework("utest.runner.Framework"),
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "utest" % "0.4.4" % "test",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
  )
)

def preventPublication(p: Project) =
  p.settings(
    publish :=(),
    publishLocal :=(),
    publishSigned :=(),
    publishLocalSigned :=(),
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", target.value / "fakepublish")),
    packagedArtifacts := Map.empty)

lazy val boopickle = crossProject
  .settings(commonSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.11.8", "2.12.0"),
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
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
  ).jsSettings(
  // use NodeJS for testing, because we need stuff like TypedArrays
    scalaJSUseRhino in Global := false,
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
  val picklers = (1 to 22).map { i =>
    def commaSeparated(s: Int => String, sep: String = ", ") = (1 to i).map(s).mkString(sep)
    val picklerTypes = commaSeparated(j => s"T$j: P")
    val typeTuple = if (i == 1) s"Tuple1[T1]" else s"(${commaSeparated(j => s"T$j")})"
    val writes = commaSeparated(j => s"write[T$j](x._$j)", "; ")
    val reads = commaSeparated(j => s"read[T$j]")

    s"""
  implicit def Tuple${i}Pickler[$picklerTypes] = new P[$typeTuple] {
    override def pickle(x: $typeTuple)(implicit state: PickleState): Unit = { $writes }
    override def unpickle(implicit state: UnpickleState) = ${if (i == 1) s"Tuple1[T1]" else ""}($reads)
  }"""
  }
  IO.write(baseDirectory.value / "target" / "TuplePicklers.scala",
    s"""package boopickle

trait TuplePicklers extends PicklerHelper {
  ${picklers.mkString("\n")}
}
""")
}

lazy val perftests = crossProject
  .settings(commonSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.11.8"),
    scalaVersion := "2.11.8",
    name := "perftests",
    scalacOptions ++= Seq("-Xstrict-inference"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % "0.3.7",
      "com.github.benhutchison" %%% "prickle" % "1.1.10",
      "com.github.fomkin" %%% "pushka-json" % "0.6.0",
      "io.circe" %%% "circe-core" % "0.4.1",
      "io.circe" %%% "circe-parser" % "0.4.1",
      "io.circe" %%% "circe-generic" % "0.4.1"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )
  .jsSettings(
    bootSnippet := "BooApp().main();",
    // scalaJSOptimizerOptions in (Compile, fullOptJS) ~= { _.withUseClosureCompiler(false) },
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.0",
      "com.lihaoyi" %%% "scalatags" % "0.5.5"
    )
  )

lazy val perftestsJS = preventPublication(perftests.js).settings(workbenchSettings: _*).dependsOn(boopickleJS)

lazy val perftestsJVM = preventPublication(perftests.jvm)
  .settings(
    libraryDependencies += "io.circe" %% "circe-jawn" % "0.2.1"
  )
  .dependsOn(boopickleJVM)

lazy val root = preventPublication(project.in(file(".")))
  .settings(
    crossScalaVersions := Seq("2.11.8", "2.12.0"),
    scalaVersion := "2.11.8"
  )
  .aggregate(boopickleJS, boopickleJVM, perftestsJS, perftestsJVM)
