package io.scalac.tezos.translator.model

import io.scalac.tezos.translator.model.LibraryEntry.Status
import io.scalac.tezos.translator.repository.dto.SendEmailDbDto
import io.scalac.tezos.translator.routes.dto.{LibraryEntryRoutesDto, SendEmailRoutesDto}

import scala.util.Try

sealed abstract case class SendEmail(
  uid: Uid,
  to: EmailAddress,
  subject: String,
  content: EmailContent
)

object SendEmail {

  def approvalRequest(libraryDto: LibraryEntryRoutesDto): SendEmail = {
    val uid = Uid()
    val subject = "Library approval request"
    val message = TextContent {
      s"""
        |Please add my translation to your library:
        |Title: ${libraryDto.name}
        |Description: ${libraryDto.description}
        |Uid: ${uid.value}
      """.stripMargin
    }
    new SendEmail(Uid(), AdminEmail, subject, message) {}
  }

  def statusChange(emailAddress: UserEmail, newStatus: Status): SendEmail = {
    val uid = Uid()
    val subject = "Acceptance status of your Translation has changed"
    val message = TextContent(s"Acceptance status of your Translation has changed to: $newStatus")

    new SendEmail(uid, emailAddress, subject, message) {}
  }

  def fromSendEmailRoutesDto(dto: SendEmailRoutesDto): SendEmail =
    new SendEmail(
      uid = Uid(),
      to = AdminEmail,
      subject = "Contact request",
      content = ContactFormContent(
        name = dto.name,
        phone = dto.phone,
        email = dto.email,
        content = dto.content
      )
    ) {}

  def fromSendEmailDbDto(dto: SendEmailDbDto): Try[SendEmail] =
    for {
      uid <- Uid.fromString(dto.uid)
      c <- EmailContent.fromJson(dto.content)
    } yield
      new SendEmail(
        uid = uid,
        to = emailAddressFromString(dto.to),
        subject = dto.subject,
        content = c
      ) {}

  private def emailAddressFromString(s: String): EmailAddress = s match {
    case "admin" => AdminEmail
    case other => UserEmail(other)
  }

}
