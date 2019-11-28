package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.repository.UserRepository
import slick.jdbc.MySQLProfile.api._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random
import com.github.t3hnar.bcrypt._
import io.scalac.tezos.translator.model.UserModel

class UserService(repository: UserRepository, db: Database) {

  private val tokenToUser = new java.util.concurrent.ConcurrentHashMap[String, String].asScala

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

  def authenticate(user: Option[UserModel], password: String): Boolean = {
    if (user.isDefined)
      password.isBcrypted(user.get.passwordHash)
    else
      false
  }

  def authenticateAndCreateToken(username: String, password: String): Future[Option[String]] = {
    db.run(repository.getByUsername(username))
      .map(user => authenticate(user.headOption, password))
      .map(if (_) Some(createToken(username)) else None)
  }

  def getUserByToken(token: String): Option[String] = tokenToUser.get(token)

}
