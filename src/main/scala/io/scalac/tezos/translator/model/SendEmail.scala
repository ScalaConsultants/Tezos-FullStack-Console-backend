package io.scalac.tezos.translator.model

import io.scalac.tezos.translator.model.LibraryEntry.Status
import io.scalac.tezos.translator.model.types.Library.Title
import io.scalac.tezos.translator.model.types.UUIDs.SendEmailId
import io.scalac.tezos.translator.model.types.UUIDs.generateSendEmailId
import io.scalac.tezos.translator.repository.dto.SendEmailDbDto
import io.scalac.tezos.translator.routes.dto.{LibraryEntryRoutesDto, SendEmailRoutesDto}

import scala.util.{Success, Try}

sealed abstract case class SendEmail(
   uid: SendEmailId,
   to: EmailAddress,
   subject: String,
   content: EmailContent)

object SendEmail {

  def approvalRequest(libraryDto: LibraryEntryRoutesDto, adminEmail: EmailAddress): SendEmail = {
    val uid     = generateSendEmailId
    val subject = "Library approval request"
    val message = TextContent {
      s"""
         |Please add my translation to your library:
         |Title: ${libraryDto.title}
         |Description: ${libraryDto.description}
         |Uid: $uid
      """.stripMargin
    }
    new SendEmail(uid, adminEmail, subject, message) {}
  }

  def statusChange(emailAddress: EmailAddress, title: Title, newStatus: Status): SendEmail = {
    val uid     = generateSendEmailId
    val subject = "Acceptance status of your Translation has changed"
    val message = TextContent(s"""Acceptance status of your translation: "$title" has changed to: $newStatus""")

    new SendEmail(uid, emailAddress, subject, message) {}
  }

  def fromSendEmailRoutesDto(dto: SendEmailRoutesDto, adminEmail: EmailAddress): Try[SendEmail] =
    for {
      email <- dto.email match {
                case Some(e) => EmailAddress.fromString(e.v.value).map(Some(_))
                case None    => Success(None)
              }
      contact <- Contact.create(dto.phone, email)
    } yield
      new SendEmail(uid     = generateSendEmailId,
                    to      = adminEmail,
                    subject = "Contact request",
                    content = ContactFormContent(name = dto.name, contact = contact, content = dto.content)) {}

  def fromSendEmailDbDto(dto: SendEmailDbDto): Try[SendEmail] =
    for {
      c  <- EmailContent.fromJson(dto.content)
      to <- EmailAddress.fromString(dto.to)
    } yield new SendEmail(uid = dto.uid, to = to, subject = dto.subject, content = c) {}

}
