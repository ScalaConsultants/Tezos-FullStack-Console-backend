package io.scalac.tezos.translator.routes.dto

import io.scalac.tezos.translator.model.LibraryEntry.PendingApproval
import io.scalac.tezos.translator.model.{LibraryEntry, Uid}

case class LibraryEntryRoutesDto(
  name: String,
  author: String,
  email: Option[String],
  description: String,
  micheline: String,
  michelson: String
) {
  def toDomain: LibraryEntry = LibraryEntry(
    uid = Uid(),
    name = name,
    author = author,
    email = email,
    description = description,
    micheline = micheline,
    michelson = michelson,
    status = PendingApproval
  )
}

object LibraryEntryRoutesDto {
  def fromDomain(v: LibraryEntry): LibraryEntryRoutesDto =
    LibraryEntryRoutesDto(
      name = v.name,
      author = v.author,
      email = v.email,
      description = v.description,
      micheline = v.micheline,
      michelson = v.michelson
    )
}
