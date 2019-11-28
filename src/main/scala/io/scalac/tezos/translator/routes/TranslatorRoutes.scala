package io.scalac.tezos.translator.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.Translation
import io.scalac.tezos.translator.routes.util.ReCaptchaDirective._
import io.scalac.tezos.translator.routes.util.Translator
import io.scalac.tezos.translator.service.TranslationsService

class TranslatorRoutes(
  translationsService: TranslationsService,
  translator: Translator,
  log: LoggingAdapter,
  reCaptchaConfig: CaptchaConfig
)(implicit as: ActorSystem) extends HttpRoutes {

  override def routes: Route =
    (pathPrefix("translate") & withReCaptchaVerify(log, reCaptchaConfig)(as)) {
      (path("from" / "michelson" / "to" / "micheline") & post & entity(as[String])) { body =>
        translator.michelson2micheline(body).fold(
          error => complete(StatusCodes.BadRequest, error.toString),
          parsed => {
            translationsService.addTranslation(Translation.FromMichelson, body, parsed)
            complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), parsed)))
          }
        )
      } ~ (path("from" / "micheline" / "to" / "michelson") & post & entity(as[String])) { body =>
        translator.micheline2michelson(body).fold(
          error => complete(StatusCodes.BadRequest, error.toString),
          parsed => {
            translationsService.addTranslation(Translation.FromMicheline, body, parsed)
            complete(parsed)
          }
        )
      }
    }

}
