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

libraryDependencies ++= Vector(
  "org.typelevel" %% "cats-core" % "2.12.0",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.scalamock" %% "scalamock" % "6.0.0" % Test
)

Test / test :=
  (Test / test)
    .dependsOn(Compile / scalafmtCheck)
    .dependsOn(Test / scalafmtCheck)
    .value
