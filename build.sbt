import Dependencies._

name := "Tezos-FullStack-Console"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Scalac" at "https://raw.githubusercontent.com/ScalaConsultants/mvn-repo/master/"

val akkaHttpVersion = "10.1.10"

libraryDependencies ++= akkaHttp ++ akkaHttpCors ++ akkaStream ++ jodaTime ++ logBack ++ mySql ++ scalactic ++
                        scalaTest ++ sl4j ++ slick ++ tesozFCTM

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