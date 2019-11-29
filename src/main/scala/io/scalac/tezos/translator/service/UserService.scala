package io.scalac.tezos.translator.service

import com.github.t3hnar.bcrypt._
import io.scalac.tezos.translator.model.UserModel
import io.scalac.tezos.translator.repository.UserRepository
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random

class UserService(repository: UserRepository, db: Database) {

  private val tokenToUser = new scala.collection.concurrent.TrieMap[String, String]

  implicit private val ec: ExecutionContextExecutor = ExecutionContext.global

  private def createToken(username: String): String = {
    def createUniqueToken = {
      var newToken = Random.alphanumeric.take(30).mkString
      while (tokenToUser.contains(newToken)) {
        newToken = Random.alphanumeric.take(30).mkString
      }
      newToken
    }

    val token = createUniqueToken
    tokenToUser.put(token, username)
    token
  }

  def authenticate(user: UserModel, password: String): Boolean = {
    password.isBcrypted(user.passwordHash)
  }

  def authenticateAndCreateToken(username: String, password: String): Future[Option[String]] = {
    db.run(repository.getByUsername(username))
      .map { userOption =>
        val isAuthenticated = userOption.exists(user => authenticate(user, password))
        if (isAuthenticated) Some(createToken(username)) else None
      }
  }

  def getUserByToken(token: String): Option[String] = tokenToUser.get(token)

}
