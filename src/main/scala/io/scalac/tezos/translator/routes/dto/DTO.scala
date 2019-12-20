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

  sealed trait ErrorDTO

  case class Error(error: String) extends ErrorDTO
  case class Errors(errors: List[String]) extends ErrorDTO

  implicit val errorsEncoder: Encoder[Errors] = deriveEncoder[Errors]
  implicit val errorsDecoder: Decoder[Errors] = deriveDecoder[Errors]

}
