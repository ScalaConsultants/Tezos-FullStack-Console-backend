package io.scalac.tezos.translator.schema

import java.sql.Timestamp

import io.scalac.tezos.translator.model.types.UUIDs.LibraryEntryId
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto
import slick.lifted.{ProvenShape, Tag}
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlProfile.ColumnOption.NotNull

object LibraryTable {

  val library = TableQuery[LibraryTable]

}

class LibraryTable(tag: Tag) extends Table[LibraryEntryDbDto](tag, "library") {

  def uid: Rep[LibraryEntryId] = column[LibraryEntryId]("uid", NotNull, O.Unique, O.SqlType("VARCHAR(36)"))

  def name: Rep[String] = column[String]("name", NotNull, O.SqlType("TEXT"))

  def author: Rep[Option[String]] = column[Option[String]]("author", O.SqlType("TEXT"))

  def email: Rep[Option[String]] = column[Option[String]]("email", O.SqlType("TEXT"))

  def description: Rep[Option[String]] = column[Option[String]]("description", O.SqlType("TEXT"))

  def micheline: Rep[String] = column[String]("micheline", NotNull, O.SqlType("TEXT"))

  def michelson: Rep[String] = column[String]("michelson", NotNull, O.SqlType("TEXT"))

  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at", NotNull, O.SqlType("TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)"))

  def status: Rep[Int] = column[Int]("status", NotNull, O.SqlType("SMALLINT"))

  override def * : ProvenShape[LibraryEntryDbDto] = (uid, name, author, email, description, micheline, michelson, createdAt, status) <> ((LibraryEntryDbDto.apply _).tupled, LibraryEntryDbDto.unapply)
}
