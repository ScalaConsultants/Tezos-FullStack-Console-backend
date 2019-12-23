package io.scalac.tezos.translator.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route
import io.scalac.tezos.translator.model.UserCredentials
import io.scalac.tezos.translator.routes.Endpoints.ErrorResponse
import io.scalac.tezos.translator.routes.directives.DTOValidationDirective
import io.scalac.tezos.translator.routes.dto.DTO.{Error, ErrorDTO}
import io.scalac.tezos.translator.service.UserService
import cats.syntax.either._
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._


import scala.concurrent.{ExecutionContext, Future}

class LoginRoutes(userService: UserService, log: LoggingAdapter)(implicit as: ActorSystem, ec: ExecutionContext) extends HttpRoutes {


  val loginEndpoint: Endpoint[UserCredentials, ErrorResponse, String, Nothing] =
    endpoint
      .post
      .in("login")
      .in(jsonBody[UserCredentials])
      .errorOut(jsonBody[ErrorDTO].and(statusCode))
      .out(jsonBody[String])


  def loginLogic(credentials: UserCredentials): Future[Either[ErrorResponse, String]] =
    userService.authenticateAndCreateToken(credentials.username, credentials.password).map {
      case Some(token) => Right(token)
      case None => (Error("Wrong credentials !"), StatusCode.Forbidden).asLeft
    }

  val loginRoute: Route = loginEndpoint.toRoute {
    (DTOValidationDirective.validateDto[UserCredentials] _).andThenFirstE(loginLogic)
  }

  def logoutLogic(token: String): Future[Either[ErrorResponse, Unit]] =
    userService.authenticate(token).map(_.map { case  (_, t) => userService.logout(t) })

  val logoutEndpoint: Endpoint[String, ErrorResponse, Unit, Nothing] =
    endpoint
      .post
      .in("logout")
      .in(auth.bearer)
      .errorOut(jsonBody[ErrorDTO].and(statusCode))
      .out(statusCode(StatusCode.Ok))

  val logoutRoute: Route = logoutEndpoint.toRoute(logoutLogic)

  override def routes: Route =
    loginRoute ~ logoutRoute

  override def docs: List[Endpoint[_, _, _, _]] = List(loginEndpoint, logoutEndpoint)

}
