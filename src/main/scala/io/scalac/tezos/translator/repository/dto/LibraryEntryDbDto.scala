package io.scalac.tezos.translator.repository.dto

import java.sql.Timestamp
import java.time.Instant

import io.scalac.tezos.translator.model.LibraryEntry.Status
import io.scalac.tezos.translator.model.{AuthorName, EmailAddress, LibraryEntry, Uid}

import scala.util.{Success, Try}

case class LibraryEntryDbDto(
                              uid: String,
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
      uid <- Uid.fromString(uid)
      emailAdress <- email match {
        case Some(e) => EmailAddress.fromString(e).map(Some(_))
        case None => Success(None)
      }
    } yield
      LibraryEntry(
        uid = uid,
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
      uid = v.uid.value,
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