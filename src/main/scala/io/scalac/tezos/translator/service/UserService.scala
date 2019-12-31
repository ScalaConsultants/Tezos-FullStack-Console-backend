package io.scalac.tezos.translator.service

import cats.syntax.either._
import com.github.t3hnar.bcrypt._
import eu.timepit.refined._
import io.scalac.tezos.translator.model.types.Auth.{Password, UserToken, UserTokenReq, Username}
import io.scalac.tezos.translator.model.{AuthUserData, UserModel}
import io.scalac.tezos.translator.repository.UserRepository
import io.scalac.tezos.translator.routes.Endpoints.ErrorResponse
import io.scalac.tezos.translator.routes.dto.DTO.Error
import scalacache.Cache
import scalacache.modes.scalaFuture._
import slick.jdbc.PostgresProfile.api._
import sttp.model.StatusCode

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class UserService(repository: UserRepository, tokenToUser: Cache[Username], db: Database)(implicit ec: ExecutionContext) {

  private def createToken(username: Username): Future[UserToken] = {
    val newToken = generateRandomToken
    tokenToUser.caching(newToken.v.value)(None)(username).map(_ => newToken)
  }

  @tailrec
  private def generateRandomToken: UserToken = {
    val maybeNewTokenEntry = refineV[UserTokenReq](Random.alphanumeric.take(30).mkString)
    maybeNewTokenEntry match {
      case Left(_) => generateRandomToken
      case Right(value) => UserToken(value)
    }
  }

  private def checkPassword(user: UserModel, password: Password): Boolean = {
    password.v.value.isBcrypted(user.passwordHash.v.value)
  }

  def authenticateAndCreateToken(username: Username, password: Password): Future[Option[UserToken]] = {
    for {
      maybeUser <- db.run(repository.getByUsername(username))
      isAuthenticated = maybeUser.exists(user => checkPassword(user, password))
      token <- if (isAuthenticated) createToken(username).map(Some(_)) else Future.successful(None)
    } yield token
  }

  def authenticate(token: UserToken): Future[Either[ErrorResponse, AuthUserData]] =
    tokenToUser.get(token.v.value).map {
      _.fold {
        (Error("Token not found"), StatusCode.Unauthorized).asLeft[AuthUserData]
      } {
        username => AuthUserData(username, token).asRight
      }
    }

  def logout(token: UserToken): Future[Unit] = tokenToUser.remove(token).map(_ => ())

}
