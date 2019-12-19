package io.scalac.tezos.translator.routes.dto

import org.joda.time.DateTime
import io.circe._, io.circe.generic.semiauto._

object DTO {

  case class CaptchaVerifyResponse(
                                    success:       Boolean,
                                    challenge_ts:  Option[DateTime],
                                    hostname:      Option[String],
                                    `error-codes`: Option[List[String]]
                                  )
  case class Error(error: String)
  case class Errors(errors: List[String])

  implicit val errorsEncoder: Encoder[Errors] = deriveEncoder[Errors]
  implicit val errorsDecoder: Decoder[Errors] = deriveDecoder[Errors]

}
