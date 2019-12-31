package io.scalac.tezos.translator.routes.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import io.circe.Decoder
import io.scalac.tezos.translator.routes.dto.DTO.{CaptchaVerifyResponse, Error}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import akka.event.LoggingAdapter
import cats.syntax.either._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.types.Auth.Captcha
import io.scalac.tezos.translator.routes.Endpoints.ErrorResponse
import sttp.model.StatusCode
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object ReCaptcha {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  def withReCaptchaVerify(maybeCaptcha: Option[Captcha],
                          log: LoggingAdapter,
                          reCaptchaConfig: CaptchaConfig)
                         (implicit actorSystem: ActorSystem,
                          ec: ExecutionContext): Future[Either[ErrorResponse, Unit]] =
    if (reCaptchaConfig.checkOn)
      maybeCaptcha
        .fold {
          captchaHeaderIsMissingAsFuture(reCaptchaConfig.headerName)
        } {
          header => checkCaptcha(header, log, reCaptchaConfig)
        }
    else
      Future.successful(().asRight[ErrorResponse])

  protected def captchaHeaderIsMissingAsFuture(captchaHeaderName: String): Future[Either[ErrorResponse, Unit]] =
    Future
      .successful(
        (Error(s"Request is missing required HTTP header '$captchaHeaderName'"), StatusCode.BadRequest).asLeft
      )

  protected def checkCaptcha(userCaptcha: Captcha,
                             log: LoggingAdapter,
                             reCaptchaConfig: CaptchaConfig)
                            (implicit actorSystem: ActorSystem,
                             ec: ExecutionContext): Future[Either[ErrorResponse, Unit]] = {

      implicit val materializer: ActorMaterializer = ActorMaterializer()

      for {
        verifyResponse <- doRequestToVerifyCaptcha(userCaptcha, log, reCaptchaConfig)
        result <- verifyResponse match {
          case Right(response) => checkVerifyResponse(response, log)
          case Left(error)     => Future.successful((error, StatusCode.BadRequest).asLeft)
        }
      } yield result
  }

  protected def doRequestToVerifyCaptcha(userCaptcha: Captcha,
                                         log: LoggingAdapter,
                                         reCaptchaConfig: CaptchaConfig)
                                        (implicit actorSystem: ActorSystem,
                                         ec: ExecutionContext): Future[Either[Error, HttpResponse]] = {
    Http().singleRequest(
      request = HttpRequest(
        HttpMethods.POST,
        reCaptchaConfig.url + s"?secret=${reCaptchaConfig.secret}&response=${userCaptcha.v.value}"
      )
    )
      .map(Right(_))
      .recover {
        case err =>
          log.error(s"Can't do request to verify captcha - $userCaptcha, err - $err")
          Left(Error("Can't do request to verify captcha"))
      }
  }

  protected def checkVerifyResponse(response: HttpResponse,
                                    log: LoggingAdapter)
                                   (implicit am: ActorMaterializer,
                                    ec: ExecutionContext): Future[Either[ErrorResponse, Unit]] = {

    val dateFormatter = DateTimeFormat.forPattern("yyyyMMdd")
    implicit val decodeDateTime: Decoder[DateTime] = Decoder.decodeString.emap { s =>
      try {
        DateTime.parse(s, dateFormatter).asRight
      } catch {
        case NonFatal(e) => e.getMessage.asLeft
      }
    }

    val unmarshalResult = Unmarshal(response).to[CaptchaVerifyResponse]
    unmarshalResult.map { value =>
      if (value.success)
        ().asRight
      else
        (Error("Invalid captcha"), StatusCode.Unauthorized).asLeft
    }.recover {
      case err =>
        log.error(s"Can't parse google reCaptcha response, err - $err")
        (Error("Can't parse captcha response from google"), StatusCode.InternalServerError).asLeft
    }
  }

}
