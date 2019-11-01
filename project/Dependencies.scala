import sbt._

object Version {

  val akkaHttp     = "10.1.10"
  val akkaHttpCors = "0.4.1"
  val akkaStream   = "2.5.23"
  val cats         = "2.0.0"
  val jodaTime     = "2.10.4"
  val logback      = "1.2.3"
  val mySql        = "8.0.17"
  val pureConfig   = "0.12.1"
  val scalactic    = "3.0.5"
  val scalaTest    = "3.0.5"
  val sl4j         = "1.7.26"
  val slick        = "3.3.1"
  val tesozFCTM    = "0.1"

}

object Dependencies {

  val akkaHttp: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-http"            % Version.akkaHttp,
    "com.typesafe.akka" %% "akka-http-spray-json" % Version.akkaHttp,
    "com.typesafe.akka" %% "akka-http-testkit"    % Version.akkaHttp % "test"
  )

  val akkaHttpCors: Seq[ModuleID] = Seq(
    "ch.megard" %% "akka-http-cors" % Version.akkaHttpCors
  )

  val akkaStream: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-stream"         % Version.akkaStream,
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.akkaStream % "test"
  )

  val cats: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % Version.cats
  )

  val jodaTime: Seq[ModuleID] = Seq (
    "joda-time" % "joda-time" % "2.10.4"
  )

  val logBack: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % Version.logback
  )

  val mySql: Seq[ModuleID] = Seq(
    "mysql" % "mysql-connector-java" % Version.mySql
  )

  val pureConfig: Seq[ModuleID] = Seq(
    "com.github.pureconfig" %% "pureconfig" % Version.pureConfig
  )

  val scalactic: Seq[ModuleID] = Seq(
    "org.scalactic" %% "scalactic" % Version.scalactic
  )

  val scalaTest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % Version.scalaTest % "test"
  )

  val sl4j: Seq[ModuleID] = Seq(
    "org.slf4j" % "slf4j-api" % Version.sl4j
  )

  val slick: Seq[ModuleID] = Seq(
    "com.typesafe.slick" %% "slick"          % Version.slick,
    "com.typesafe.slick" %% "slick-hikaricp" % Version.slick
  )

  val tesozFCTM: Seq[ModuleID] = Seq(
    "io.scalac" %% "tezos-fullstack-console-translation-module" % Version.tesozFCTM
  )

}
