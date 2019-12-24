package io.scalac.tezos.translator.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.routes.directives.ReCaptchaDirective._
import io.scalac.tezos.translator.routes.util.Translator

class TranslatorRoutes(
  translator: Translator,
  log: LoggingAdapter,
  reCaptchaConfig: CaptchaConfig
)(implicit as: ActorSystem) extends HttpRoutes {

  override def routes: Route =
    pathPrefix("translate")  {
      (path("from" / "michelson" / "to" / "micheline") & post & entity(as[String])) { body =>
        translator.michelson2micheline(body).fold(
          error => {
            log.error(s"Michelson -> Micheline translation failure. Reason: $error")
            complete(StatusCodes.BadRequest, "invalid syntax")
          },
          parsed => complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), parsed)))
        )
      } ~ (path("from" / "micheline" / "to" / "michelson") & post & entity(as[String])) { body =>
        translator.micheline2michelson(body).fold(
          error => {
            log.error(s"Micheline -> Michelson translation failure. Reason: ${error.toString}")
            complete(StatusCodes.BadRequest, "input json cannot be parsed")
          },
          parsed => complete(parsed)
        )
      }
    }

}
