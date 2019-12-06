package io.scalac.tezos.translator.repository.dto

import java.sql.Timestamp
import java.time.Instant

import io.scalac.tezos.translator.model.SendEmail._
import io.scalac.tezos.translator.model.{SendEmail, Uid}

import scala.util.Try

case class SendEmailDbDto(
  uid: String,
  from: String,
  to: String,
  subject: String,
  content: String,
  createdAt: Timestamp
) {

  def toDomain: Try[SendEmail] =
    for {
      uid <- Uid.fromString(uid)
      c <- Content.fromJson(content)
    } yield
      SendEmail(
        uid = uid,
        from = EmailAddress.fromString(from),
        to = EmailAddress.fromString(to),
        subject = subject,
        content = c
      )

}

object SendEmailDbDto {

  def fromDomain(v: SendEmail): SendEmailDbDto =
    SendEmailDbDto(
      uid = v.uid.value,
      from = EmailAddress.toString(v.from),
      to = EmailAddress.toString(v.to),
      subject = v.subject,
      content = Content.toJson(v.content),
      createdAt = Timestamp.from(Instant.now)
    )

}
