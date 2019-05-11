import sbt._
import Keys._
import com.typesafe.sbt.pgp.PgpKeys._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

ThisBuild / scalafmtOnCompile := true

val commonSettings = Seq(
  organization := "io.suzaku",
  version := Version.library,
  crossScalaVersions := Seq("2.11.12", "2.12.6", "2.13.0-RC1"),
  scalaVersion in ThisBuild := "2.12.6",
  scalacOptions := Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("-Xlint:-unused")
    case Some((2, 12)) => Seq("-Xfatal-warnings", "-Xfuture", "-Xlint:-unused", "-Yno-adapted-args")
    case _             => Seq("-Xfatal-warnings", "-Xfuture", "-Yno-adapted-args")
  }),
  Compile / scalacOptions ~= (_ filterNot (_ == "-Ywarn-value-discard")),
  unmanagedSourceDirectories in Compile ++= {
    (unmanagedSourceDirectories in Compile).value.map { dir =>
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => file(dir.getPath ++ "-2.13+")
        case _             => file(dir.getPath ++ "-2.13-")
      }
    }
  },
  testFrameworks += new TestFramework("utest.runner.Framework"),
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "utest" % "0.6.7" % Test,
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
  )
)

val nativeSettings = Seq(
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.11.12"),
  nativeLinkStubs := true,
  // Disable Scaladoc generation because of:
  // [error] dropping dependency on node with no phase object: mixin
  Compile / doc / sources := Seq.empty
)

val releaseSettings = Seq(
  scmInfo := Some(
    ScmInfo(url("https://github.com/suzaku-io/boopickle"),
            "scm:git:git@github.com:suzaku-io/boopickle.git",
            Some("scm:git:git@github.com:suzaku-io/boopickle.git"))),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomExtra :=
    <url>https://github.com/suzaku-io/boopickle</url>
      <licenses>
        <license>
          <name>Apache 2.0 license</name>
          <url>http://www.opensource.org/licenses/Apache-2.0</url>
        </license>
      </licenses>
      <developers>
        <developer>
          <id>ochrons</id>
          <name>Otto Chrons</name>
          <url>https://github.com/ochrons</url>
        </developer>
      </developers>,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

val sourceMapSettings = Seq(
  scalacOptions ++= (if (isSnapshot.value) Seq.empty
                     else
                       Seq({
                         val a = baseDirectory.value.toURI.toString.replaceFirst("[^/]+/?$", "")
                         val g = "https://raw.githubusercontent.com/suzaku-io/boopickle"
                         s"-P:scalajs:mapSourceURI:$a->$g/v${version.value}/"
                       }))
)

def preventPublication(p: Project) =
  p.settings(
    publish := (()),
    publishLocal := (()),
    publishSigned := (()),
    publishLocalSigned := (()),
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", target.value / "fakepublish")),
    packagedArtifacts := Map.empty
  )

lazy val boopickle = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(commonSettings)
  .settings(releaseSettings)
  .settings(
    name := "boopickle"
  )
  .jsSettings(sourceMapSettings)
  .jvmSettings()
  .nativeSettings(nativeSettings)

lazy val boopickleJS = boopickle.js
lazy val boopickleJVM = boopickle.jvm
lazy val boopickleNative = boopickle.native

lazy val shapeless = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .dependsOn(boopickle)
  .settings(commonSettings)
  .settings(releaseSettings)
  .settings(
    name := "boopickle-shapeless",
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.3"
    )
  )
  .jsSettings(sourceMapSettings)
  .jvmSettings()
  .nativeSettings(nativeSettings)

lazy val shapelessJS = shapeless.js
lazy val shapelessJVM = shapeless.jvm
lazy val shapelessNative = shapeless.native

lazy val generateTuples = taskKey[Unit]("Generates source code for pickling tuples")

/*
Generator for all the 22 Tuple picklers. Resulting source files are written under `target`
from where they need to be copied to the `boopickle` source directory.
 */
generateTuples := {
  val picklers = (1 to 22).map { i =>
    def commaSeparated(s: Int => String, sep: String = ", ") = (1 to i).map(s).mkString(sep)

    val picklerTypes = commaSeparated(j => s"T$j: P")
    val typeTuple    = if (i == 1) s"Tuple1[T1]" else s"(${commaSeparated(j => s"T$j")})"
    val writes       = commaSeparated(j => s"write[T$j](x._$j)", "; ")
    val reads        = commaSeparated(j => s"read[T$j]")

    s"""
  implicit def Tuple${i}Pickler[$picklerTypes] = new P[$typeTuple] {
    override def pickle(x: $typeTuple)(implicit state: PickleState): Unit = { $writes }
    override def unpickle(implicit state: UnpickleState) = ${if (i == 1) s"Tuple1[T1]" else ""}($reads)
  }"""
  }
  IO.write(
    baseDirectory.value / "target" / "TuplePicklers.scala",
    s"""package boopickle

trait TuplePicklers extends PicklerHelper {
  ${picklers.mkString("\n")}
}
"""
  )
}

lazy val perftests = crossProject(JSPlatform, JVMPlatform)
  .settings(commonSettings)
  .settings(
    name := "perftests",
    scalaVersion := "2.12.6",
    scalacOptions ++= Seq("-Xstrict-inference"),
    libraryDependencies ++= Seq(
      "com.lihaoyi"       %%% "upickle"       % "0.5.1",
      "com.typesafe.play" %%% "play-json"     % "2.6.9",
      "io.circe"          %%% "circe-core"    % "0.9.2",
      "io.circe"          %%% "circe-parser"  % "0.9.2",
      "io.circe"          %%% "circe-generic" % "0.9.2"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
  )
  .enablePlugins(JmhPlugin)
  .jsSettings(
    scalaJSOptimizerOptions in (Compile, fullOptJS) ~= { _.withUseClosureCompiler(false) },
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.6",
      "com.lihaoyi"  %%% "scalatags"   % "0.6.7"
    )
  )

lazy val perftestsJS = preventPublication(perftests.js).enablePlugins(WorkbenchPlugin).dependsOn(boopickleJS)

lazy val perftestsJVM = preventPublication(perftests.jvm)
  .settings(
    libraryDependencies += "io.circe" %% "circe-jawn" % "0.9.2"
  )
  .dependsOn(boopickleJVM)

lazy val booPickleRoot = preventPublication(project.in(file(".")))
  .settings(commonSettings)
  .aggregate(boopickleJS, boopickleJVM, boopickleNative, shapelessJS, shapelessJVM, shapelessNative)
