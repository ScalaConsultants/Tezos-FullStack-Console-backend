package io.scalac.tezos.translator.model

import java.sql.Timestamp
import java.time.Instant

import scala.util.Try

case class SendEmail(
  uid: Uid,
  name: String,
  phone: String,
  email: String,
  content: String
) {

  def toDbDTO: SendEmailDbDTO = SendEmailDbDTO(
    id = None,
    uid = uid.value,
    name = name,
    phone = phone,
    email = email,
    content = content,
    createdAt = Timestamp.from(Instant.now)
  )

  def toJsonDTO: SendEmailJsonDTO = SendEmailJsonDTO(
    name: String,
    phone: String,
    email: String,
    content: String
  )
}

object SendEmail {
  def fromDbDto(dto: SendEmailDbDTO): Try[SendEmail] =
    Uid.fromString(dto.uid).map { v =>
      SendEmail(
        uid = v,
        name = dto.name,
        phone = dto.phone,
        email = dto.email,
        content = dto.content
      )
    }

  def fromJsonDto(dto: SendEmailJsonDTO) =
    SendEmail(
      uid = Uid(),
      name = dto.name,
      phone = dto.phone,
      email = dto.email,
      content = dto.content
    )
}

