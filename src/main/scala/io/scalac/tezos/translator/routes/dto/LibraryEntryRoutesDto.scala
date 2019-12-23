package io.scalac.tezos.translator.routes.dto

import cats.syntax.flatMap
import io.scalac.tezos.translator.model.LibraryEntry.{PendingApproval, Status}
import io.scalac.tezos.translator.model.{EmailAddress, LibraryEntry, Uid}

import scala.util.{Success, Try}

case class LibraryEntryRoutesDto(
  title: String,
  author: Option[String],
  email: Option[String],
  description: Option[String],
  micheline: String,
  michelson: String) {

  def toDomain: Try[LibraryEntry] = {

    val emailAdress = email match {
      case Some(e) => EmailAddress.fromString(e).map(Some(_))
      case None    => Success(None)
    }

    emailAdress.map(email => {

      LibraryEntry(uid = Uid(),
                   title = title,
                   author = author,
                   email = email,
                   description = description,
                   micheline = micheline,
                   michelson = michelson,
                   status = PendingApproval)
    })
  }

}

object LibraryEntryRoutesDto {

  def fromDomain(v: LibraryEntry): LibraryEntryRoutesDto =
    LibraryEntryRoutesDto(title = v.title,
                          author = v.author,
                          email = v.email.map(_.toString),
                          description = v.description,
                          micheline = v.micheline,
                          michelson = v.michelson)

}
