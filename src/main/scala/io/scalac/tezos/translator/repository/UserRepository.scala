package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.schema.UserTable
import slick.jdbc.MySQLProfile.api._

class UserRepository {

  def userExists(username: String, passwordHash: String): DBIO[Boolean] =
    UserTable.users
      .filter(_.username === username)
      .filter(_.passwordHash === passwordHash)
      .exists
      .result
}
