package io.scalac.tezos.translator.schema

import java.sql.Timestamp

import io.scalac.tezos.translator.model.types.ContactData.EmailS
import io.scalac.tezos.translator.model.types.Library._
import io.scalac.tezos.translator.model.types.UUIDs.LibraryEntryId
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto
import slick.lifted.{ ProvenShape, Tag }
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlProfile.ColumnOption.NotNull

object LibraryTable {

  val library = TableQuery[LibraryTable]

}

class LibraryTable(tag: Tag) extends Table[LibraryEntryDbDto](tag, "library") {

  override def * : ProvenShape[LibraryEntryDbDto] =
    (uid, title, author, email, description, micheline, michelson, createdAt, status) <> ((LibraryEntryDbDto.apply _).tupled, LibraryEntryDbDto.unapply)

  def uid: Rep[LibraryEntryId] = column[LibraryEntryId]("uid", NotNull, O.Unique, O.SqlType("VARCHAR(36)"))

  def title: Rep[Title] = column[Title]("name", NotNull, O.SqlType("TEXT"))

  def author: Rep[Option[Author]] = column[Option[Author]]("author", O.SqlType("TEXT"))

  def email: Rep[Option[EmailS]] = column[Option[EmailS]]("email", O.SqlType("TEXT"))

  def description: Rep[Option[Description]] = column[Option[Description]]("description", O.SqlType("TEXT"))

  def micheline: Rep[Micheline] = column[Micheline]("micheline", NotNull, O.SqlType("TEXT"))

  def michelson: Rep[Michelson] = column[Michelson]("michelson", NotNull, O.SqlType("TEXT"))

  def createdAt: Rep[Timestamp] =
    column[Timestamp]("created_at", NotNull, O.SqlType("TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)"))

  def status: Rep[Int] = column[Int]("status", NotNull, O.SqlType("SMALLINT"))
}
