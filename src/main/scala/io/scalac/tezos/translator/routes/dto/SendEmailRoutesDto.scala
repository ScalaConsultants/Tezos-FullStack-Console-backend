package io.scalac.tezos.translator.routes.dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.scalac.tezos.translator.model.types.ContactData._

case class SendEmailRoutesDto(
                               name: Name,
                               phone: Option[Phone],
                               email: Option[RefinedEmailString],
                               content: Content
                             )

object SendEmailRoutesDto {

  implicit val SendEmailRoutesDtoEncoder: Encoder[SendEmailRoutesDto] = deriveEncoder[SendEmailRoutesDto]
  implicit val SendEmailRoutesDtoDecoder: Decoder[SendEmailRoutesDto] = deriveDecoder[SendEmailRoutesDto]

}
