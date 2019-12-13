package io.scalac.tezos.translator.routes.dto

import io.scalac.tezos.translator.model.LibraryEntry

case class LibraryEntryRoutesAdminDto(
  uid: String,
  title: String,
  author: Option[String],
  email: Option[String],
  description: Option[String],
  micheline: String,
  michelson: String,
  status: String,
)

object LibraryEntryRoutesAdminDto {
  def fromDomain(v: LibraryEntry): LibraryEntryRoutesAdminDto = {
    LibraryEntryRoutesAdminDto(
      uid = v.uid.value,
      title = v.title,
      author = v.author.map(_.toString),
      email = v.email.map(_.toString),
      description = v.description.map(_.toString),
      micheline = v.micheline,
      michelson = v.michelson,
      status = v.status.toString
    )
  }
}
