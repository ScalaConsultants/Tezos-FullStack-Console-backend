package io.scalac.tezos.translator.routes.dto

import io.scalac.tezos.translator.model.{SendEmail, Uid}

case class SendEmailRoutesDto(
  name: String,
  phone: String,
  email: String,
  content: String
) {

  def toDomain: SendEmail =
    SendEmail(
      uid = Uid(),
      name = name,
      phone = phone,
      email = email,
      content = content
    )
}

object SendEmailRoutesDto {

  def fromDomain(v: SendEmail): SendEmailRoutesDto =
    SendEmailRoutesDto(
      name = v.name,
      phone = v.phone,
      email = v.email,
      content = v.content
    )
}