package io.scalac.tezos.translator.routes.dto

case class SendEmailRoutesDto(
  name: String,
  phone: Option[String],
  email: Option[String],
  content: String
)
