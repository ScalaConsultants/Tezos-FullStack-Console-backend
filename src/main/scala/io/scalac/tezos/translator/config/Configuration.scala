package io.scalac.tezos.translator.config

import akka.event.LoggingAdapter
import pureconfig._
import pureconfig.generic.auto._ // Required, don't let idea optimize imports here, otherwise it will delete that import

case class Configuration(reCaptcha: CaptchaConfig = CaptchaConfig())

case class CaptchaConfig(checkOn: Boolean   = false,
                         url: String        = "https://www.google.com/recaptcha/api/siteverify",
                         secret: String     = "??",
                         headerName: String = "Captcha")


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
