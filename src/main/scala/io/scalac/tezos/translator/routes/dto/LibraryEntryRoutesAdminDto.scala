package io.scalac.tezos.translator.routes.dto

import io.scalac.tezos.translator.model.LibraryEntry

case class LibraryEntryRoutesAdminDto(
  uid: String,
  name: String,
  author: String,
  email: Option[String],
  description: String,
  micheline: String,
  michelson: String,
  status: String,
)

object LibraryEntryRoutesAdminDto {
  def fromDomain(v: LibraryEntry): LibraryEntryRoutesAdminDto = {
    LibraryEntryRoutesAdminDto(
      uid = v.uid.value,
      name = v.name,
      author = v.author,
      email = v.email,
      description = v.description,
      micheline = v.micheline,
      michelson = v.michelson,
      status = v.status.toString
    )
  }
}
