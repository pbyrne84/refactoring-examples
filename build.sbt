lazy val baseName = "refactoring-examples"

val scala3Version = "3.5.1"

scalaVersion := scala3Version
scalacOptions ++= Seq(
  "-encoding",
  "utf8",
  "-feature",
  "-no-indent",
  "-language:implicitConversions",
  "-language:existentials",
  "-unchecked"
)

Test / test :=
  (Test / test)
    .dependsOn(Compile / scalafmtCheck)
    .dependsOn(Test / scalafmtCheck)
    .value
