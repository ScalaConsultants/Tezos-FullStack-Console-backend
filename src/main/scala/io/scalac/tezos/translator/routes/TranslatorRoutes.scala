package io.scalac.tezos.translator.routes

import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route
import cats.syntax.either._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.routes.TranslatorRoutes._
import io.scalac.tezos.translator.routes.util.Translator
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._
import scala.concurrent.{ExecutionContext, Future}

class TranslatorRoutes(
  translator: Translator,
  log: LoggingAdapter,
  reCaptchaConfig: CaptchaConfig
)(implicit ec: ExecutionContext) extends HttpRoutes {

  override def routes: Route =
    fromMichelsonEndpoint.toRoute(
      body =>
        Future(
          translator
            .michelson2micheline(body)
            .leftMap((StatusCode.BadRequest, _))
        )
    ) ~ fromMichelineEndpoint.toRoute(
      body =>
        Future(
          translator
            .micheline2michelson(body)
            .leftMap(error => (StatusCode.BadRequest, error.toString))
        )
    )

}

object TranslatorRoutes {
  val translationEndpoint
  : Endpoint[String, (StatusCode, String), String, Nothing] =
    endpoint
      .post
      .in("translate")
      .errorOut(statusCode.and(jsonBody[String]))
      .in(stringBody)
      .out(stringBody)

  val fromMichelsonEndpoint
  : Endpoint[String, (StatusCode, String), String, Nothing] =
    translationEndpoint.in("from" / "michelson" / "to" / "micheline")

  val fromMichelineEndpoint
  : Endpoint[String, (StatusCode, String), String, Nothing] =
    translationEndpoint.in("from" / "micheline" / "to" / "michelson")
}
