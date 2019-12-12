package io.scalac.tezos.translator.routes.dto

import io.scalac.tezos.translator.model.LibraryEntry.PendingApproval
import io.scalac.tezos.translator.model.{EmailAddress, LibraryEntry, Uid}

import scala.util.{Success, Try}

case class LibraryEntryRoutesDto(
  name: String,
  author: String,
  email: Option[String],
  description: String,
  micheline: String,
  michelson: String
) {
  def toDomain: Try[LibraryEntry] = {
    val emailAddress = email match {
      case Some(v) => EmailAddress.fromString(v).map(Some(_))
      case None => Success(None)
    }

    emailAddress.map { ea =>
      LibraryEntry(
        uid = Uid(),
        name = name,
        author = author,
        email = ea,
        description = description,
        micheline = micheline,
        michelson = michelson,
        status = PendingApproval
      )
    }
  }
}

object LibraryEntryRoutesDto {
  def fromDomain(v: LibraryEntry): LibraryEntryRoutesDto =
    LibraryEntryRoutesDto(
      name = v.name,
      author = v.author,
      email = v.email.map(_.toString),
      description = v.description,
      micheline = v.micheline,
      michelson = v.michelson
    )
}
