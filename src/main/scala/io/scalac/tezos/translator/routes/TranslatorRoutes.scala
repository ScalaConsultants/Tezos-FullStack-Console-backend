package io.scalac.tezos.translator.routes

import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import io.scalac.tezos.translator.micheline.MichelineTranslator
import io.scalac.tezos.translator.michelson.JsonToMichelson
import io.scalac.tezos.translator.michelson.dto.MichelsonSchema
import io.scalac.tezos.translator.model.Translation
import io.scalac.tezos.translator.service.TranslationsService

class TranslatorRoutes(translationsService: TranslationsService) extends HttpRoutes {

  override def routes: Route =
    pathPrefix("translate") {
      path("from" / "michelson" / "to" / "micheline") {
        post {
          entity(as[String]) { body =>
            MichelineTranslator.michelsonToMicheline(body).fold(
              error => complete(StatusCodes.BadRequest, error.toString),
              parsed => {
                translationsService.addTranslation(Translation.FromMichelson, body, parsed)
                complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), parsed)))
              }
            )
          }
        }
      } ~ path("from" / "micheline" / "to" / "michelson") {
        post {
          entity(as[String]) { body =>
            JsonToMichelson.convert[MichelsonSchema](body).fold(
              error => complete(StatusCodes.BadRequest, error.toString),
              parsed => {
                translationsService.addTranslation(Translation.FromMicheline, body, parsed)
                complete(parsed)
              }
            )
          }
        }
      }
    }

}
