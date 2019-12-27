package io.scalac.tezos.translator.routes

import cats.data.EitherT
import cats.instances.future._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.routes.dto.DTO.{Error, ErrorDTO}
import sttp.tapir.{Endpoint, endpoint, header, jsonBody, statusCode}
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import cats.syntax.option._
import io.scalac.tezos.translator.service.UserService
import scala.concurrent.{ExecutionContext, Future}

object Endpoints {

  type ErrorResponse = (ErrorDTO, StatusCode)

  def baseEndpoint: Endpoint[Unit, Unit, Unit, Nothing] =
    endpoint.in("v1")

  def captchaEndpoint(reCaptchaConfig: CaptchaConfig): Endpoint[Option[String], ErrorResponse, Unit, Nothing] =
    baseEndpoint.in(header[Option[String]](reCaptchaConfig.headerName)).errorOut(jsonBody[ErrorDTO].and(statusCode))

  val errorResponse: EndpointOutput[ErrorResponse] = jsonBody[ErrorDTO].and(statusCode)

  val offsetQuery: EndpointInput.Query[Option[Int]] =
    query[Option[Int]]("offset").description("Offset").example(40.some)
  val limitQuery: EndpointInput.Query[Option[Int]] =
    query[Option[Int]]("limit").description("Limit").example(20.some)
  val uidQuery: EndpointInput.Query[String] =
    query[String]("uid").description("Uid").example("4a5c1b83-c263-49be-95d2-620e4a832e94")
  val statusQuery: EndpointInput.Query[String] =
    query[String]("status").description("Desired status").example("accepted")

  val maybeAuthHeader: EndpointIO.Header[Option[String]] =
    header[Option[String]]("Authorization")
      .description("Optional authorization header")
      .example("Bearer  WcPvrwuCTJYghiz2vxQsvmOzmPA9uH".some)

  private val bearer: String = "Bearer "

  implicit class OptionAuthOps(val maybeString: Option[String]) extends AnyVal {
    def withMaybeAuth[R](userService: UserService)
                        (onAuth:      => Future[Either[ErrorResponse, R]])
                        (withoutAuth: => Future[Either[ErrorResponse, R]])
                        (implicit ec: ExecutionContext): EitherT[Future, ErrorResponse, R] =
      maybeString
        .fold(EitherT(withoutAuth)) {
          string =>
            if (string.startsWith(bearer) && string.length > bearer.length) {
              val token = string.drop(bearer.length)
              for {
                _      <- EitherT(userService.authenticate(token))
                result <- EitherT(onAuth)
              } yield result
            } else
              EitherT.leftT[Future, R]((Error("Invalid Authorization header"), StatusCode.BadRequest))
        }
  }

}
