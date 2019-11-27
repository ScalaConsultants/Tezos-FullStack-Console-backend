package io.scalac.tezos.translator.schema

import java.sql.Timestamp

import io.scalac.tezos.translator.model.LibraryDbDTO
import slick.lifted.{ProvenShape, Tag}
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlProfile.ColumnOption.Nullable

object LibraryTable {

  val library = TableQuery[LibraryTable]

}

class LibraryTable(tag: Tag) extends Table[LibraryDbDTO](tag, "library") {

  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def uid: Rep[String] = column[String]("uid", O.SqlType("VARCHAR(8) NOT NULL"))

  def name: Rep[String] = column[String]("name", O.SqlType("TINYTEXT NOT NULL"))

  def author: Rep[String] = column[String]("author", O.SqlType("TINYTEXT NOT NULL"))

  def email: Rep[Option[String]] = column[Option[String]]("email", O.SqlType("TINYTEXT"))

  def description: Rep[String] = column[String]("description", O.SqlType("TEXT NOT NULL"))

  def micheline: Rep[String] = column[String]("micheline", O.SqlType("TEXT NOT NULL"))

  def michelson: Rep[String] = column[String]("michelson", O.SqlType("TEXT NOT NULL"))

  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at", slick.sql.SqlProfile.ColumnOption.NotNull, O.SqlType("TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)"))

  def status: Rep[Int] = column[Int]("status", Nullable, O.SqlType("INT(2) NOT NULL"))

  override def * : ProvenShape[LibraryDbDTO] = (id.?, uid, name, author, email, description, micheline, michelson, createdAt, status) <> (LibraryDbDTO.tupled, LibraryDbDTO.unapply)
}
