package io.scalac.tezos.translator.repository.dto

import java.sql.Timestamp
import java.time.Instant

import io.scalac.tezos.translator.model.types.UUIDs.SendEmailId
import io.scalac.tezos.translator.model.{ EmailContent, SendEmail }

case class SendEmailDbDto(
   uid: SendEmailId,
   to: String,
   subject: String,
   content: String,
   createdAt: Timestamp)

object SendEmailDbDto {

  def fromDomain(v: SendEmail): SendEmailDbDto =
    SendEmailDbDto(uid       = v.uid,
                   to        = v.to.toString,
                   subject   = v.subject,
                   content   = EmailContent.toJson(v.content),
                   createdAt = Timestamp.from(Instant.now))

}
