package io.scalac.tezos.translator.service

import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.Provided
import com.github.t3hnar.bcrypt._
import io.scalac.tezos.translator.model.UserModel
import io.scalac.tezos.translator.repository.UserRepository
import io.scalac.tezos.translator.routes.dto.DTO.{Error, ErrorDTO}
import slick.jdbc.PostgresProfile.api._
import cats.syntax.either._
import sttp.model.StatusCode

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class UserService(repository: UserRepository, db: Database)(implicit ec: ExecutionContext) {

  private val tokenToUser = new scala.collection.concurrent.TrieMap[String, String]

  @tailrec
  private def createToken(username: String): String = {
    val newToken = Random.alphanumeric.take(30).mkString
    tokenToUser.putIfAbsent(newToken, username) match {
      case None => newToken
      case Some(_) => createToken(username) // token already exists, retry
    }
  }

  private def checkPassword(user: UserModel, password: String): Boolean = {
    password.isBcrypted(user.passwordHash)
  }

  def authenticateAndCreateToken(username: String, password: String): Future[Option[String]] = {
    db.run(repository.getByUsername(username))
      .map { userOption =>
        val isAuthenticated = userOption.exists(user => checkPassword(user, password))
        if (isAuthenticated) Some(createToken(username)) else None
      }
  }

  def authenticate(token: String): Future[Either[(ErrorDTO, StatusCode), (String, String)]] = Future {
    tokenToUser.get(token)
      .fold {
        (Error("Token not found"), StatusCode.Unauthorized).asLeft[(String, String)]
      } {
        x => (x, token).asRight
      }
  }

  def logout(token: String): Unit = tokenToUser.remove(token)

}
