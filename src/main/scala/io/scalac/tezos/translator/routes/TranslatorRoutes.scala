package io.scalac.tezos.translator.routes

import akka.http.scaladsl.server.Route
import cats.syntax.either._
import io.scalac.tezos.translator.routes.TranslatorRoutes._
import io.scalac.tezos.translator.routes.utils.Translator
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._
import scala.concurrent.{ ExecutionContext, Future }

class TranslatorRoutes(translator: Translator)(implicit ec: ExecutionContext) extends HttpRoutes {

  override def routes: Route =
    fromMichelsonEndpoint.toRoute(
       body =>
         Future(
            translator
              .michelson2micheline(body)
              .leftMap(_ => (StatusCode.BadRequest, "invalid syntax"))
         )
    ) ~ fromMichelineEndpoint.toRoute(
       body =>
         Future(
            translator
              .micheline2michelson(body)
              .leftMap(_ => (StatusCode.BadRequest, "input json cannot be parsed"))
         )
    )

  override def docs: List[Endpoint[_, _, _, _]] = List(fromMichelsonEndpoint, fromMichelineEndpoint)

}

object TranslatorRoutes {

  private val translationEndpoint: Endpoint[String, (StatusCode, String), String, Nothing] =
    Endpoints.baseEndpoint.post
      .in("translate")
      .errorOut(statusCode.and(jsonBody[String]))
      .in(stringBody)
      .out(stringBody)

  private val fromMichelsonEndpoint: Endpoint[String, (StatusCode, String), String, Nothing] =
    translationEndpoint.in("from" / "michelson" / "to" / "micheline")

  private val fromMichelineEndpoint: Endpoint[String, (StatusCode, String), String, Nothing] =
    translationEndpoint.in("from" / "micheline" / "to" / "michelson")
}
