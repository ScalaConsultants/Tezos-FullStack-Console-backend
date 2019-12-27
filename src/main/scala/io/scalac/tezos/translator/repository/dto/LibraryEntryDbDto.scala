package io.scalac.tezos.translator.repository.dto

import java.sql.Timestamp
import java.time.Instant

import io.scalac.tezos.translator.model.LibraryEntry.Status
import io.scalac.tezos.translator.model.types.UUIDs.LibraryEntryId
import io.scalac.tezos.translator.model.{EmailAddress, LibraryEntry}

import scala.util.{Success, Try}

case class LibraryEntryDbDto(
                              id: LibraryEntryId,
                              title: String,
                              author: Option[String],
                              email: Option[String],
                              description: Option[String],
                              micheline: String,
                              michelson: String,
                              createdAt: Timestamp,
                              status: Int = 1
                            ) {

  def toDomain: Try[LibraryEntry] =
    for {
      status <- Status.fromInt(status)
      emailAdress <- email match {
        case Some(e) => EmailAddress.fromString(e).map(Some(_))
        case None => Success(None)
      }
    } yield
      LibraryEntry(
        uid = id,
        title = title,
        author = author,
        email = emailAdress,
        description = description,
        micheline = micheline,
        michelson = michelson,
        status = status
      )
}

object LibraryEntryDbDto {
  def fromDomain(v: LibraryEntry) =
    LibraryEntryDbDto(
      id = v.uid,
      title = v.title,
      author = v.author.map(_.toString),
      email = v.email.map(_.toString),
      description = v.description.map(_.toString),
      micheline = v.micheline,
      michelson = v.michelson,
      createdAt = Timestamp.from(Instant.now),
      status = v.status.value
    )
}