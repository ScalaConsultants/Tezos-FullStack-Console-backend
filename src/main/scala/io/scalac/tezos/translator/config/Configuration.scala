package io.scalac.tezos.translator.config

import pureconfig.ConfigReader.Result
import pureconfig._
import pureconfig.generic.auto._ // Required, don't let idea optimize imports here, otherwise it will delete that import

case class Configuration(reCaptcha: CaptchaConfig = CaptchaConfig())

case class CaptchaConfig(checkOn: Boolean   = false,
                         url: String        = "https://www.google.com/recaptcha/api/siteverify",
                         secret: String     = "??",
                         headerName: String = "Captcha")


object Configuration {

  lazy val config: Result[Configuration] = ConfigSource
    .default
    .load[Configuration]


}
