package io.scalac.tezos.translator.model

import org.joda.time.DateTime

object DTO {

  case class CaptchaVerifyResponse(success: Boolean,
                                   challenge_ts: DateTime,
                                   hostname: String,
                                   `error-codes`: List[String])
  case class ErrorDTO(error: String)
  case class SendEmailDTO(name: String, phone: String, email: String, content: String)

}
