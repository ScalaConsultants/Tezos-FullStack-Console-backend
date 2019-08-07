package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.stream.ActorMaterializer
import io.scalac.tezos.translator.micheline.MichelineTranslator
import io.scalac.tezos.translator.michelson.JsonToMichelson
import io.scalac.tezos.translator.michelson.dto.MichelsonSchema
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._


import scala.concurrent.Future

object Routes {

  val route =
    cors() {
      pathPrefix("v1") {
        pathPrefix("translate") {
          path("from" / "michelson" / "to" / "micheline") {
            post {
              entity(as[String]) { body =>
                MichelineTranslator.michelsonToMicheline(body).fold(
                  error => complete(StatusCodes.BadRequest, error.toString),
                  parsed => complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), parsed)))
                )
              }
            }
          } ~ path("from" / "micheline" / "to" / "michelson") {
            post {
              entity(as[String]) { body =>
                JsonToMichelson.convert[MichelsonSchema](body).fold(
                  error => complete(StatusCodes.BadRequest, error.toString),
                  parsed => complete(parsed)
                )
              }
            }
          }
        }
      }
    }

  def setupRoutes(host: String, port: Int)(
    implicit actorSystem: ActorSystem,
    actorMaterializer: ActorMaterializer): Future[Http.ServerBinding] = {

    Http().bindAndHandle(route, host, port)
  }

}
