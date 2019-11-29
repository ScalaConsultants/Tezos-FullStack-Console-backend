package io.scalac.tezos.translator.schema

import io.scalac.tezos.translator.model.UserModel
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}
import slick.sql.SqlProfile.ColumnOption.NotNull

object UsersTable {
  val users = TableQuery[UsersTable]
}

class UsersTable(tag: Tag) extends Table[UserModel](tag, "users") {

  def username: Rep[String] = column[String]("username"/*, NotNull, O.Unique, O.SqlType("TEXT")*/)

  def passwordHash: Rep[String] = column[String]("password_hash"/*, NotNull, O.SqlType("TEXT")*/)

  override def * : ProvenShape[UserModel] = (username, passwordHash) <> (UserModel.tupled, UserModel.unapply)

}
