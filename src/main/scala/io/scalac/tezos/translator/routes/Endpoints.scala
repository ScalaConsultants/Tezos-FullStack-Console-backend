package io.scalac.tezos.translator.routes

import cats.data.EitherT
import cats.instances.future._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.routes.dto.DTO.{Error, ErrorDTO}
import io.scalac.tezos.translator.service.UserService
import io.scalac.tezos.translator.model.types.Auth.{Captcha, UserToken, UserTokenReq}
import io.scalac.tezos.translator.model.types.Params.{Limit, Offset}
import io.scalac.tezos.translator.model.types.UUIDs._
import sttp.tapir.{Endpoint, endpoint, header, jsonBody, statusCode}
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import cats.syntax.option._
import cats.syntax.flatMap._
import cats.syntax.either._
import scala.concurrent.{ExecutionContext, Future}
import eu.timepit.refined._
import eu.timepit.refined.string.Uuid

object Endpoints {

  type ErrorResponse = (ErrorDTO, StatusCode)

  val offset = "offset"
  val limit  = "limit"

  def baseEndpoint: Endpoint[Unit, Unit, Unit, Nothing] =
    endpoint.in("v1")

  def captchaEndpoint(reCaptchaConfig: CaptchaConfig): Endpoint[Option[Captcha], ErrorResponse, Unit, Nothing] =
    baseEndpoint.in(header[Option[Captcha]](reCaptchaConfig.headerName)).errorOut(jsonBody[ErrorDTO].and(statusCode))

  implicit class OptionAuthOps(val maybeToken: Option[UserToken]) extends AnyVal {
    def withMaybeAuth[R](userService: UserService)
                        (onAuth:      => Future[Either[ErrorResponse, R]])
                        (withoutAuth: => Future[Either[ErrorResponse, R]])
                        (implicit ec: ExecutionContext): EitherT[Future, ErrorResponse, R] =
      maybeToken.fold(EitherT(withoutAuth))(token => EitherT(userService.authenticate(token)) >> EitherT(onAuth))
  }

  def bearer2TokenF(value: String): Future[Either[ErrorResponse, UserToken]] =
    Future.successful(bearer2Token(value))

  def bearer2Token(value: String): Either[ErrorResponse, UserToken] =
    refineV[UserTokenReq](value).bimap(er => (Error(er), StatusCode.BadRequest), UserToken.apply)


  val errorResponse: EndpointOutput[ErrorResponse] = jsonBody[ErrorDTO].and(statusCode)

  val offsetQuery: EndpointInput.Query[Option[Offset]] =
    query[Option[Offset]](offset).description("Offset")
  val limitQuery: EndpointInput.Query[Option[Limit]] =
    query[Option[Limit]](limit).description("Limit")
  val uidQuery: EndpointInput.Query[UUIDString] =
    query[UUIDString]("uid").description("Uid").example(refineMV[Uuid]("8682fcd8-b73d-48fa-abdc-3a2b9093b1f2"))
  val statusQuery: EndpointInput.Query[String] =
    query[String]("status").description("Desired status").example("accepted")

  val maybeAuthHeader: EndpointIO.Header[Option[UserToken]] =
    header[Option[UserToken]]("Authorization")
      .description("Optional authorization header")
      .example(
        UserToken(
          refineMV[UserTokenReq]("WcPvrwuCTJYghiz2vxQsvmOzmPA9uH")
        ).some
      )

}
