package io.scalac.tezos.translator.config

import pureconfig._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._ // Required, don't let idea optimize imports here, otherwise it will delete that import

import scala.concurrent.duration._
import scala.language.postfixOps

case class Configuration(
  reCaptcha: CaptchaConfig = CaptchaConfig(),
  email: EmailConfiguration = EmailConfiguration(),
  cron: CronConfiguration = CronConfiguration(),
  dbUtility: DBUtilityConfiguration = DBUtilityConfiguration(),
  dbEvolutionConfig: DbEvolutionConfig
)

case class CaptchaConfig(
  checkOn: Boolean = false,
  url: String = "https://www.google.com/recaptcha/api/siteverify",
  secret: String = "??",
  headerName: String = "CAPTCHA"
)

case class EmailConfiguration(
  host: String = "smtp.gmail.com",
  port: Int = 587,
  auth: Boolean = true,
  user: String = "you@gmail.com",
  pass: String = "p@$$w3rd",
  startTls: Boolean = true,
  receiver: String = "enterYours@gmail.com"
)


case class CronConfiguration(
  cronBatchSize: Int = 10,
  startDelay: FiniteDuration = 0 milliseconds,
  cronTaskInterval: FiniteDuration = 30 seconds
)

case class DBUtilityConfiguration(defaultLimit: Int = 10)

case class DbEvolutionConfig(
  url: String,
  user: String,
  password: String,
  migrationScriptsPackage: String,
  enabled: Boolean
)

object Configuration {

  def getConfig: Either[ConfigReaderFailures, Configuration] =
    ConfigSource.default.load[Configuration]

}
