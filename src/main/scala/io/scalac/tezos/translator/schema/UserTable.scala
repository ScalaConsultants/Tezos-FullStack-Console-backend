package io.scalac.tezos.translator.schema

import io.scalac.tezos.translator.model.UserModel
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}

object UserTable {
  val users = TableQuery[UserTable]
}

class UserTable(tag: Tag) extends Table[UserModel](tag, "users") {

  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def username: Rep[String] = column[String]("username")

  def passwordHash: Rep[String] = column[String]("passwordHash")

  override def * : ProvenShape[UserModel] = (id, username, passwordHash) <> (UserModel.tupled, UserModel.unapply)

}
