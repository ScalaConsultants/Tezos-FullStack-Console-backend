package io.scalac.tezos.translator.routes.dto

import io.scalac.tezos.translator.model.SendEmail.{AdminEmail, ContactFormContent, MmtServiceEmail}
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
      from = MmtServiceEmail,
      to = AdminEmail,
      subject = "Contact request",
      content = ContactFormContent(
        name: String,
        phone: String,
        email: String,
        content: String
      )
    )
}
