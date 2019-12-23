package io.scalac.tezos.translator.routes

import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.routes.dto.DTO.ErrorDTO
import sttp.model.StatusCode
import sttp.tapir.json.circe._
import sttp.tapir.{Endpoint, endpoint, header, jsonBody, statusCode}
import io.circe.generic.auto._

object Endpoints {

  type ErrorResponse = (ErrorDTO, StatusCode)

  def baseEndpoint: Endpoint[Unit, Unit, Unit, Nothing] =
    endpoint.in("v1")

  def captchaEndpoint(reCaptchaConfig: CaptchaConfig): Endpoint[String, ErrorResponse, Unit, Nothing] =
    baseEndpoint.in(header[String](reCaptchaConfig.headerName)).errorOut(jsonBody[ErrorDTO].and(statusCode))

}
