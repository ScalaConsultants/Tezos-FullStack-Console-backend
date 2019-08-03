package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.stream.ActorMaterializer
import io.scalac.tezos.translator.michelson.JsonToMichelson

import scala.concurrent.Future

object Routes {

  val route =
    pathPrefix("v1") {
      pathPrefix("translate") {
        path("from" / "michelson" / "to" / "micheline") {
          post {
            entity(as[String]) { body =>
              complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), Samples.micheline)))
            }
          }
        } ~ path("from" / "micheline" / "to" / "michelson") {
          post {
            entity(as[String]) { body =>
              complete(Samples.michelson)
            }
          }
        }
      }
    }

  def setupRoutes (
    implicit actorSystem: ActorSystem,
    actorMaterializer: ActorMaterializer): Future[Http.ServerBinding] = {

    Http().bindAndHandle(route, "localhost", 8080)
  }

}
