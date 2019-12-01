package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.model.UserModel
import io.scalac.tezos.translator.schema.UsersTable
import slick.jdbc.PostgresProfile.api._

class UserRepository {

  def getByUsername(username: String): DBIO[Option[UserModel]] =
    UsersTable.users
      .filter(_.username === username)
      .take(1)
      .result
      .headOption
}
