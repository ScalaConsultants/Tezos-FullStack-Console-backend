package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.EmailAddress
import io.scalac.tezos.translator.routes._
import io.scalac.tezos.translator.routes.util.Translator
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, UserService}

import scala.concurrent.ExecutionContext

class Routes(
  emails2SendService: Emails2SendService,
  libraryService: LibraryService,
  userService: UserService,
  translator: Translator,
  log: LoggingAdapter,
  captchaConfig: CaptchaConfig,
  adminEmail: EmailAddress
)(implicit as: ActorSystem, ec: ExecutionContext) {

  private val apis: List[HttpRoutes] =
    List(
      new TranslatorRoutes(translator, log, captchaConfig),
      new MessageRoutes(emails2SendService, log, captchaConfig, adminEmail),
      new LibraryRoutes(libraryService, userService, emails2SendService, log, captchaConfig, adminEmail),
      new LoginRoutes(userService, log)
    )

  lazy val allRoutes: Route =
    cors() {
      pathPrefix("v1") {
        apis.map(_.routes).reduce(_ ~ _)
      }
    }

}
