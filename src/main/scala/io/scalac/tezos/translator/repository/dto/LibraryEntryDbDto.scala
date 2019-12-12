package io.scalac.tezos.translator.repository.dto

import java.sql.Timestamp
import java.time.Instant

import io.scalac.tezos.translator.model.{EmailAddress, LibraryEntry, Uid}
import io.scalac.tezos.translator.model.LibraryEntry.Status

import scala.util.{Success, Try}

case class LibraryEntryDbDto(
  uid: String,
  name: String,
  author: String,
  email: Option[String],
  description: String,
  micheline: String,
  michelson: String,
  createdAt: Timestamp,
  status: Int = 1
) {

  def toDomain: Try[LibraryEntry] =
    for {
      status        <-  Status.fromInt(status)
      uid           <-  Uid.fromString(uid)
      emailAddress  <-  email match {
                          case Some(e) => EmailAddress.fromString(e).map(Some(_))
                          case None => Success(None)
                        }
    } yield
      LibraryEntry(
        uid = uid,
        name = name,
        author = author,
        email = emailAddress,
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
      name = v.name,
      author = v.author,
      email = v.email.map(_.toString),
      description = v.description,
      micheline = v.micheline,
      michelson = v.michelson,
      createdAt = Timestamp.from(Instant.now),
      status = v.status.value
    )
}