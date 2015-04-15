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
