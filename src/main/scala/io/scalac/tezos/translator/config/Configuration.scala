package io.scalac.tezos.translator.config

import akka.event.LoggingAdapter
import pureconfig._
import pureconfig.generic.auto._ // Required, don't let idea optimize imports here, otherwise it will delete that import
import scala.concurrent.duration._
import scala.language.postfixOps

case class Configuration(reCaptcha: CaptchaConfig     = CaptchaConfig(),
                         email: EmailConfiguration    = EmailConfiguration(),
                         cron: CronConfiguration      = CronConfiguration())

case class CaptchaConfig(checkOn: Boolean   = false,
                         url: String        = "https://www.google.com/recaptcha/api/siteverify",
                         secret: String     = "??",
                         headerName: String = "Captcha")

case class EmailConfiguration(host: String          = "smtp.gmail.com",
                              port: Int             = 587,
                              auth: Boolean         = true,
                              user: String          = "you@gmail.com",
                              pass: String          = "p@$$w3rd",
                              startTls: Boolean     = true,
                              receiver: String      = "enterYours@gmail.com",
                              subjectPrefix: String = "Service message")


case class CronConfiguration(cronBatchSize: Int               = 50,
                             startDelay: FiniteDuration       = 0 milliseconds,
                             cronTaskInterval: FiniteDuration = 30 seconds)

object Configuration {

  def getConfig(log: LoggingAdapter): Configuration = {
    ConfigSource
      .default
      .load[Configuration]
      .getOrElse {
        val defaultConfig = Configuration()
        log.error(s"Can't load config from config file. Loading default config - $defaultConfig")
        defaultConfig
      }
  }

}
