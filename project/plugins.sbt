val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("1.3.0")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")

addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.0.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.3.7")

//addSbtPlugin("com.lihaoyi" % "workbench" % "0.4.1")

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.4")

addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.5")
