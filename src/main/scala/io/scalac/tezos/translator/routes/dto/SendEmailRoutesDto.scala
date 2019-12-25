package io.scalac.tezos.translator.routes.dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class SendEmailRoutesDto(
  name: String,
  phone: Option[String],
  email: Option[String],
  content: String
)

object SendEmailRoutesDto {

  implicit val SendEmailRoutesDtoEncoder: Encoder[SendEmailRoutesDto] = deriveEncoder[SendEmailRoutesDto]
  implicit val SendEmailRoutesDtoDecoder: Decoder[SendEmailRoutesDto] = deriveDecoder[SendEmailRoutesDto]

}
