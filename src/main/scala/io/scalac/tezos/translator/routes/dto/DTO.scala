package io.scalac.tezos.translator.routes.dto

import org.joda.time.DateTime
import io.circe._, io.circe.generic.semiauto._

object DTO {

  case class CaptchaVerifyResponse(
                                    success: Boolean,      // whether this request was a valid reCAPTCHA token for your site
                                    score:  Int ,           // the score for this request (0.0 - 1.0)
                                    action: String,            // the action name for this request (important to verify)
                                    challenge_ts:  Option[DateTime],  // timestamp of the challenge load (ISO format yyyy-MM-dd'T'HH:mm:ssZZ)
                                    hostname:      Option[String],   // the hostname of the site where the reCAPTCHA was solved
                                    `error-codes`: Option[List[String]]    // optional
  )



  sealed trait ErrorDTO

  case class Error(error: String) extends ErrorDTO
  case class Errors(errors: List[String]) extends ErrorDTO

  implicit val errorEncoder: Encoder[Error]   = deriveEncoder[Error]
  implicit val errorDecoder: Decoder[Error]   = deriveDecoder[Error]
  implicit val errorsEncoder: Encoder[Errors] = deriveEncoder[Errors]
  implicit val errorsDecoder: Decoder[Errors] = deriveDecoder[Errors]

  implicit val errorDTOEncoder: Encoder[ErrorDTO] = {
    case v: Error  => errorEncoder.apply(v)
    case v: Errors => errorsEncoder.apply(v)
  }

}
