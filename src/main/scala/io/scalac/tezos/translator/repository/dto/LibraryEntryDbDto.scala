package io.scalac.tezos.translator.repository.dto

import java.sql.Timestamp
import java.time.Instant

import io.scalac.tezos.translator.model.LibraryEntry.Status
import io.scalac.tezos.translator.model.types.ContactData.EmailS
import io.scalac.tezos.translator.model.types.ContactData.MaybeEmailAddressOps
import io.scalac.tezos.translator.model.types.Library._
import io.scalac.tezos.translator.model.types.UUIDs.LibraryEntryId
import io.scalac.tezos.translator.model.{ EmailAddress, LibraryEntry }

import scala.util.{ Success, Try }

case class LibraryEntryDbDto(
   uid: LibraryEntryId,
   title: Title,
   author: Option[Author],
   email: Option[EmailS],
   description: Option[Description],
   micheline: Micheline,
   michelson: Michelson,
   createdAt: Timestamp,
   status: Int = 1) {

  def toDomain: Try[LibraryEntry] =
    for {
      status <- Status.fromInt(status)
      emailAdress <- email match {
                      case Some(e) => EmailAddress.fromString(e.v.value).map(Some(_))
                      case None    => Success(None)
                    }
    } yield LibraryEntry(uid         = uid,
                         title       = title,
                         author      = author,
                         email       = emailAdress,
                         description = description,
                         micheline   = micheline,
                         michelson   = michelson,
                         status      = status)
}

object LibraryEntryDbDto {

  def fromDomain(v: LibraryEntry) =
    LibraryEntryDbDto(uid         = v.uid,
                      title       = v.title,
                      author      = v.author,
                      email       = v.email.toEmailS,
                      description = v.description,
                      micheline   = v.micheline,
                      michelson   = v.michelson,
                      createdAt   = Timestamp.from(Instant.now),
                      status      = v.status.value)
}
