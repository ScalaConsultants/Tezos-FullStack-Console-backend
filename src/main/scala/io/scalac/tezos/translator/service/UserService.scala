package io.scalac.tezos.translator.service

import com.github.t3hnar.bcrypt._
import io.scalac.tezos.translator.model.UserModel
import io.scalac.tezos.translator.repository.UserRepository
import io.scalac.tezos.translator.routes.dto.DTO.Error
import io.scalac.tezos.translator.model.types.Auth.{UserToken, UserTokenType}
import eu.timepit.refined._
import slick.jdbc.PostgresProfile.api._
import cats.syntax.either._
import io.scalac.tezos.translator.routes.Endpoints.ErrorResponse
import sttp.model.StatusCode
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class UserService(repository: UserRepository, db: Database)(implicit ec: ExecutionContext) {

  private val tokenToUser = new scala.collection.concurrent.TrieMap[UserToken, String]

  @tailrec
  private def createToken(username: String): UserToken = {
    val newToken = generateRandomToken
    tokenToUser.putIfAbsent(newToken, username) match {
      case None => newToken
      case Some(_) => createToken(username) // token already exists, retry
    }
  }

  @tailrec
  private def generateRandomToken: UserToken = {
    val maybeNewTokenEntry = refineV[UserTokenType](Random.alphanumeric.take(30).mkString)
    maybeNewTokenEntry match {
      case Left(_)      => generateRandomToken
      case Right(value) => UserToken(value)
    }
  }

  private def checkPassword(user: UserModel, password: String): Boolean = {
    password.isBcrypted(user.passwordHash)
  }

  def authenticateAndCreateToken(username: String, password: String): Future[Option[UserToken]] = {
    db.run(repository.getByUsername(username))
      .map { userOption =>
        val isAuthenticated = userOption.exists(user => checkPassword(user, password))
        if (isAuthenticated) Some(createToken(username)) else None
      }
  }


  def authenticate(token: UserToken): Future[Either[ErrorResponse, (String, UserToken)]] = Future {
    tokenToUser.get(token)
      .fold {
        (Error("Token not found"), StatusCode.Unauthorized).asLeft[(String, UserToken)]
      } {
        username => (username, token).asRight
      }
  }

  def logout(token: UserToken): Unit = tokenToUser.remove(token)

}
