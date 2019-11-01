package io.scalac.tezos.translator.routes.util

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directive, Directives, Route, StandardRoute}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.DTO.{CaptchaVerifyResponse, ErrorDTO}
import io.scalac.tezos.translator.routes.JsonHelper

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object ReCaptchaDirective extends Directives with JsonHelper {

  def withReCaptchaVerify(log: LoggingAdapter,
                          reCaptchaConfig: CaptchaConfig)
                         (implicit actorSystem: ActorSystem): Directive[Unit] =
    headerValueByName(reCaptchaConfig.headerName)
      .flatMap(
        userCaptcha => captchaCheckDirective(userCaptcha, log, reCaptchaConfig)
      )

  protected def captchaCheckDirective(userCaptcha: String,
                                      log: LoggingAdapter,
                                      reCaptchaConfig: CaptchaConfig)
                                     (implicit actorSystem: ActorSystem): Directive[Unit] = Directive[Unit] {
    inner => ctx =>

      if (reCaptchaConfig.checkOn) {
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        implicit val ec: ExecutionContextExecutor    = ctx.executionContext

        for {
          verifyResponse <- doRequestToVerifyCaptcha(userCaptcha, log, reCaptchaConfig)
          preparedResult <- verifyResponse match {
            case Right(response) => checkVerifyResponse(response, log, inner)
            case Left(errorResponse) => Future.successful(errorResponse)
          }
          result <- preparedResult(ctx)
        } yield result
      }
      else
        inner()(ctx)

  }

  protected def doRequestToVerifyCaptcha(userCaptcha: String,
                                         log: LoggingAdapter,
                                         reCaptchaConfig: CaptchaConfig
                                        )
                                        (implicit actorSystem: ActorSystem,
                                         ec: ExecutionContext): Future[Either[StandardRoute, HttpResponse]] = {
    Http().singleRequest(
      request = HttpRequest(
        HttpMethods.POST,
        reCaptchaConfig.url + s"?secret=${reCaptchaConfig.secret}&response=$userCaptcha"
      )
    )
      .map(Right(_))
      .recover {
        case err =>
          log.error(s"Can't do request to verify captcha - $userCaptcha, err - $err")
          Left(complete(StatusCodes.InternalServerError, ErrorDTO("Can't do request to verify captcha")))
      }
  }

  protected def checkVerifyResponse(response: HttpResponse,
                                    log: LoggingAdapter,
                                    inner: Unit => Route)
                                   (implicit am: ActorMaterializer, ec: ExecutionContext): Future[Route] = {
    val unmarshalResult = Unmarshal(response).to[CaptchaVerifyResponse]
    unmarshalResult.map { value =>
      if (value.success)
        inner(())
      else
        complete(StatusCodes.Unauthorized, ErrorDTO("Invalid captcha"))
    }.recover {
      case err =>
        log.error(s"Can't parse google reCaptcha response, err - $err")
        complete(StatusCodes.InternalServerError, ErrorDTO("Can't parse captcha response from google"))
    }
  }

}
