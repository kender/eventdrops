name := "eventdrops"

organization := "me.enkode"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.1"

scalacOptions += "-feature"

val akkaVersion = "2.3.3"
val sprayVersion = "1.3.1-20140423"

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io"
)

libraryDependencies ++= Seq(
  "me.enkode" %% "js-droplets" % "0.1-SNAPSHOT",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "io.spray" %% "spray-routing" % sprayVersion,
  "io.spray" %% "spray-can" % sprayVersion
)

scalaJSSettings

Revolver.settings
