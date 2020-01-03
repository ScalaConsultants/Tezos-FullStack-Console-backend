package io.scalac.tezos.translator.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directive, Route}
import io.scalac.tezos.translator.model.UserCredentials
import io.scalac.tezos.translator.routes.directives.DTOValidationDirective._
import io.scalac.tezos.translator.service.UserService

import scala.util.{Failure, Success}

class LoginRoutes(userService: UserService, log: LoggingAdapter)(implicit as: ActorSystem) extends HttpRoutes {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  override def routes: Route =
    (pathPrefix("login") & pathEndOrSingleSlash & validateCredentialsFormat & post) { credentials =>
      onComplete(userService.authenticateAndCreateToken(credentials.username, credentials.password)) {
        case Failure(ex) =>
          log.error(s"user login failed with: ${ex.getMessage}")
          complete(HttpResponse(status = StatusCodes.InternalServerError))
        case Success(Some(token)) => complete(token)
        case _                    => complete(HttpResponse(status = StatusCodes.Forbidden))
      }
    } ~
      (pathPrefix("logout")
        & pathEndOrSingleSlash
        & authenticateOAuth2("", userService.authenticateOAuth2AndPrependUsername)
        & post) {
        case (_, token) =>
          userService.logout(token)
          complete(StatusCodes.OK)
      }

  def validateCredentialsFormat: Directive[Tuple1[UserCredentials]] = withDTOValidation[UserCredentials]
}
