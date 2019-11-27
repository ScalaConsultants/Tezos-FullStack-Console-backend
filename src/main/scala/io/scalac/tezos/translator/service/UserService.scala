package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.repository.UserRepository
import slick.jdbc.MySQLProfile.api._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class UserService(repository: UserRepository, db: Database) {

  private val tokenToUser = new java.util.concurrent.ConcurrentHashMap[String, String].asScala

  private def createAndReturnToken(username: String): String = {
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

  def authenticateAndCreateToken(username: String, password: String): Future[Option[String]] = {
    db.run(repository.userExists(username, password))
      .map(if (_) Some(createAndReturnToken(username)) else None)(ExecutionContext.global)
  }

  def getUserByToken(token: String): Option[String] = tokenToUser.get(token)

}
