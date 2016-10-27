name := "temp"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpasyncclient" % "4.1.2",
  "com.github.tomakehurst" % "wiremock" % "2.1.12" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "io.generators" % "generators-core" % "1.0" % "test",
  "org.mockito" % "mockito-core" % "2.2.7" % "test"
)