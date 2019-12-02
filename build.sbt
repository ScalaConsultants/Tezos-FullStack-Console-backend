import Dependencies._

lazy val root = (project in file(".")).settings(
  name := "Tezos-FullStack-Console",
  version := "0.1",
  scalaVersion := "2.12.8",
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  resolvers += "Scalac" at "https://raw.githubusercontent.com/ScalaConsultants/mvn-repo/master/",

  libraryDependencies ++= akkaHttp ++ akkaHttpCors ++ akkaStream ++ cats ++ courier ++ greenMail ++ jodaTime ++ logBack ++
    mySql ++ pureConfig ++ scalactic ++ scalaTest ++ sl4j ++ slick ++ tesozFCTM ++ wireMock ++ testContainers,
  
  Defaults.itSettings,

  parallelExecution in IntegrationTest := true,
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
  .enablePlugins(AssemblyPlugin)


