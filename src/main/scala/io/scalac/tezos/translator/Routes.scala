package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import io.scalac.tezos.translator.config.Configuration
import io.scalac.tezos.translator.routes._
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, UserService}

class Routes(emails2SendService: Emails2SendService,
             libraryService: LibraryService,
             userService: UserService,
             log: LoggingAdapter,
             config: Configuration)(implicit as: ActorSystem) {

  private val reCaptchaConfig = config.reCaptcha

  private val apis: List[HttpRoutes] =
    List(
      new TranslatorRoutes(log, reCaptchaConfig),
      new MessageRoutes(emails2SendService, log, reCaptchaConfig),
      new LibraryRoutes(libraryService, userService, log, config),
      new LoginRoutes(userService)
    )

  lazy val allRoutes: Route =
    cors() {
      pathPrefix("v1") {
        apis.map(_.routes).reduce(_ ~ _)
      }
    }

}
