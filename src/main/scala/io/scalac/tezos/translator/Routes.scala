package io.scalac.tezos.translator

import akka.http.scaladsl.server.Directives._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import io.scalac.tezos.translator.routes.{HistoryRoutes, HttpRoutes, TranslatorRoutes}

import scala.concurrent.ExecutionContext

class Routes(translationsService: TranslationsService)(implicit ec: ExecutionContext) {

  private val apis: List[HttpRoutes] =
    List(new TranslatorRoutes(translationsService), new HistoryRoutes(translationsService))

  lazy val allRoutes =
    cors() {
      pathPrefix("v1") {
        apis.map(_.routes).reduce(_ ~ _)
      }
    }

}
