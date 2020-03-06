package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.EmailAddress
import io.scalac.tezos.translator.routes._
import io.scalac.tezos.translator.routes.utils.Translator
import io.scalac.tezos.translator.service.{ Emails2SendService, LibraryService, UserService }

import scala.concurrent.ExecutionContext

class Routes(
   emails2SendService: Emails2SendService,
   libraryService: LibraryService,
   userService: UserService,
   translator: Translator,
   log: LoggingAdapter,
   captchaConfig: CaptchaConfig,
   adminEmail: EmailAddress
 )(implicit as: ActorSystem,
   ec: ExecutionContext) {

  lazy val allRoutes: Route =
    cors() {
      apis.map(_.routes).reduce(_ ~ _)
    } ~ docs

  lazy val docs: Route = {
    import sttp.tapir.docs.openapi._
    import sttp.tapir.openapi.circe.yaml._
    import sttp.tapir.swagger.akkahttp.SwaggerAkka

    val docs = apis.flatMap(_.docs).toOpenAPI("Tezos API", "1.0")
    new SwaggerAkka(docs.toYaml).routes
  }

  private val apis: List[HttpRoutes] =
    List(
       new TranslatorRoutes(translator),
       new MessageRoutes(emails2SendService, log, captchaConfig, adminEmail),
       new LibraryRoutes(libraryService, userService, emails2SendService, log, captchaConfig, adminEmail),
       new LoginRoutes(userService)
    )

}
