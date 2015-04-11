val boopickle = crossProject.settings(
  organization := "me.chrons",
  version := "0.0.1",
  scalaVersion := "2.11.6",
  name := "boopickle",
  scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")
).jsSettings(
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
