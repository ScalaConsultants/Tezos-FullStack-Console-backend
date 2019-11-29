package io.scalac.tezos.translator.repository.dto

import java.sql.Timestamp
import java.time.Instant

import io.scalac.tezos.translator.model.{SendEmail, Uid}

import scala.util.Try

case class SendEmailDbDto(
  uid: String,
  name: String,
  phone: String,
  email: String,
  content: String,
  createdAt: Timestamp
) {

  def toDomain: Try[SendEmail] =
    Uid.fromString(uid).map { v =>
      SendEmail(
        uid = v,
        name = name,
        phone = phone,
        email = email,
        content = content
      )
    }
}

object SendEmailDbDto {

  def fromDomain(v: SendEmail): SendEmailDbDto =
    SendEmailDbDto(
      uid = v.uid.value,
      name = v.name,
      phone = v.phone,
      email = v.email,
      content = v.content,
      createdAt = Timestamp.from(Instant.now)
    )
}