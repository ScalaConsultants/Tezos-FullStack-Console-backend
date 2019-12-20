package io.scalac.tezos.translator.model

import io.scalac.tezos.translator.model.LibraryEntry.Status
import io.scalac.tezos.translator.repository.dto.SendEmailDbDto
import io.scalac.tezos.translator.routes.dto.{LibraryEntryRoutesDto, SendEmailRoutesDto}

import scala.util.{Failure, Success, Try}

sealed abstract case class SendEmail(
                                      uid: Uid,
                                      to: EmailAddress,
                                      subject: String,
                                      content: EmailContent
                                    )

object SendEmail {
  def approvalRequest(libraryDto: LibraryEntryRoutesDto, adminEmail: EmailAddress): SendEmail = {
    val uid = Uid()
    val subject = "Library approval request"
    val message = TextContent {
      s"""
         |Please add my translation to your library:
         |Title: ${libraryDto.title}
         |Description: ${libraryDto.description}
         |Uid: ${uid.value}
      """.stripMargin
    }
    new SendEmail(Uid(), adminEmail, subject, message) {}
  }

  def statusChange(emailAddress: EmailAddress, title: String, newStatus: Status): SendEmail = {
    val uid = Uid()
    val subject = "Acceptance status of your Translation has changed"
    val message = TextContent(s"""Acceptance status of your translation: "$title" has changed to: $newStatus""")

    new SendEmail(uid, emailAddress, subject, message) {}
  }

  def fromSendEmailRoutesDto(dto: SendEmailRoutesDto, adminEmail: EmailAddress): Try[SendEmail] = {
    for {
      email   <-   dto.email match {
                   case Some(e) => EmailAddress.fromString(e).map(Some(_))
                   case None => Success(None)
                         }
      contact <- Contact.create(dto.phone, email)
    } yield
      new SendEmail(
        uid = Uid(),
        to = adminEmail,
        subject = "Contact request",
        content = ContactFormContent(
          name = dto.name,
          contact = contact,
          content = dto.content
        )
      ) {}
  }

  def fromSendEmailDbDto(dto: SendEmailDbDto): Try[SendEmail] =
    for {
      uid <- Uid.fromString(dto.uid)
      c   <- EmailContent.fromJson(dto.content)
      to  <- EmailAddress.fromString(dto.to)
    } yield
      new SendEmail(
        uid = uid,
        to = to,
        subject = dto.subject,
        content = c
      ) {}

}
