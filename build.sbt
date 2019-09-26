name := "Tezos-FullStack-Console"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Scalac" at "https://raw.githubusercontent.com/ScalaConsultants/mvn-repo/master/"

val akkaHttpVersion = "10.1.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream"          % "2.5.23",
  "ch.megard"         %% "akka-http-cors"       % "0.4.1",
  "org.scalactic"     %% "scalactic"            % "3.0.5",
  "io.scalac"         %% "tezos-fullstack-console-translation-module" % "0.1",
//  test
  "org.scalatest"     %% "scalatest"            % "3.0.5" % "test",
  "com.typesafe.akka" %% "akka-stream-testkit"  % "2.5.23" % "test",
  "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % "test",
)

// No need to run tests while building jar
test in assembly := {}
// Simple and constant jar name
assemblyJarName in assembly := s"console.jar"
// Merge strategy for assembling conflicts
assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}