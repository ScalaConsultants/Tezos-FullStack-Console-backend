package io.scalac.tezos.translator.schema

import io.scalac.tezos.translator.model.LibraryModel
import org.joda.time.DateTime
import slick.lifted.{ProvenShape, Tag}
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlProfile.ColumnOption.Nullable

object LibraryTable {

  val library = TableQuery[LibraryTable]

}

class LibraryTable(tag: Tag) extends Table[LibraryModel](tag, "library") {

  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def name: Rep[String] = column[String]("name", O.SqlType("tinytext"))

  def author: Rep[String] = column[String]("author", O.SqlType("tinytext"))

  def description: Rep[String] = column[String]("description", O.SqlType("text"))

  def micheline: Rep[String] = column[String]("micheline", O.SqlType("text"))

  def michelson: Rep[String] = column[String]("michelson", O.SqlType("text"))

  def createdAt: Rep[DateTime] = column[DateTime]("created_at", slick.sql.SqlProfile.ColumnOption.NotNull, O.SqlType("TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)"))

  def status: Rep[Int] = column[Int]("status", Nullable, O.SqlType("INT DEFAULT NULL"))

  override def * : ProvenShape[LibraryModel] = (id, name, author, description, micheline, michelson, createdAt, status.?) <> (LibraryModel.tupled, LibraryModel.unapply)
}
