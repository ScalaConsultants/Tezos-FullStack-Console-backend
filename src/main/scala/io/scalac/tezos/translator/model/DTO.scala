package io.scalac.tezos.translator.model

import org.joda.time.DateTime

object DTO {

  case class CaptchaVerifyResponse(success:       Boolean,
                                   challenge_ts:  Option[DateTime],
                                   hostname:      Option[String],
                                   `error-codes`: Option[List[String]])
  case class ErrorDTO(error: String)
  case class ErrorsDTO(errors: List[String])
  case class SendEmailDTO(name: String, phone: String, email: String, content: String)

}
