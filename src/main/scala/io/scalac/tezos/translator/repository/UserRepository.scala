package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.model.UserModel
import io.scalac.tezos.translator.schema.UserTable
import slick.jdbc.MySQLProfile.api._

class UserRepository {

  def getByUsername(username: String): DBIO[Seq[UserModel]] =
    UserTable.users
      .filter(_.username === username)
      .take(1)
      .result
}
