import Dependencies._

lazy val root = (project in file(".")).settings(
  name := "Tezos-FullStack-Console",
  version := "0.1",
  scalaVersion := "2.12.10",
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  resolvers += "Scalac" at "https://raw.githubusercontent.com/ScalaConsultants/mvn-repo/master/",

  libraryDependencies ++= akkaHttp ++ akkaHttpCors ++ akkaStream ++ cats ++ circe ++ courier ++ greenMail ++
    jodaTime ++ logBack ++ newType ++ postgres ++ pureConfig ++ refined ++ scalactic ++ scalaTest ++ sl4j ++ slick ++
    tapir ++ tesozFCTM ++ wireMock ++ testContainers ++ bcrypt ++ flyway,
  
  Defaults.itSettings,

  parallelExecution in Test := false,
  parallelExecution in IntegrationTest := false,
  // No need to run tests while building jar
  test in assembly := {},
  // Simple and constant jar name
  assemblyJarName in assembly := s"tezos-console.jar",
  // Merge strategy for assembling conflicts
  assemblyMergeStrategy in assembly := {
    case PathList("reference.conf") => MergeStrategy.concat
    case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
    case _ => MergeStrategy.first
  },
  
  scalacOptions ++= Seq("-Ypartial-unification")

).configs(IntegrationTest)

addCommandAlias("testAll", ";test;it:test")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
