package io.scalac.tezos.translator.routes

import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.routes.dto.DTO.ErrorDTO
import sttp.tapir.{Endpoint, endpoint, header, jsonBody, statusCode}
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import cats.syntax.option._

object Endpoints {

  type ErrorResponse = (ErrorDTO, StatusCode)

  def baseEndpoint: Endpoint[Unit, Unit, Unit, Nothing] =
    endpoint.in("v1")

  def captchaEndpoint(reCaptchaConfig: CaptchaConfig): Endpoint[String, ErrorResponse, Unit, Nothing] =
    baseEndpoint.in(header[String](reCaptchaConfig.headerName)).errorOut(jsonBody[ErrorDTO].and(statusCode))

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

}
