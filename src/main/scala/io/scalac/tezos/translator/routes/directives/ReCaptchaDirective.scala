package io.scalac.tezos.translator.routes.directives

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directive, Directives, StandardRoute}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import io.circe.Decoder
import io.scalac.tezos.translator.routes.dto.DTO.{CaptchaVerifyResponse, Error, ErrorDTO}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route
import cats.syntax.either._
import io.scalac.tezos.translator.config.CaptchaConfig
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

object ReCaptchaDirective extends Directives {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  def captchaEndpoint(reCaptchaConfig: CaptchaConfig): Endpoint[String, (ErrorDTO, StatusCode), Unit, Nothing] =
    endpoint.in(header[String](reCaptchaConfig.headerName)).errorOut(jsonBody[ErrorDTO].and(statusCode))

  def withReCaptchaVerify(log: LoggingAdapter,
                          reCaptchaConfig: CaptchaConfig)
                         (implicit actorSystem: ActorSystem): Directive[Unit] =
    if (reCaptchaConfig.checkOn)
      headerValueByName(reCaptchaConfig.headerName)
        .flatMap(
          userCaptcha => captchaCheckDirective(userCaptcha, log, reCaptchaConfig)
        )
    else
      Directive[Unit](inner => ctx => inner()(ctx))

  def withReCaptchaVerify1(header: String,
                           log: LoggingAdapter,
                           reCaptchaConfig: CaptchaConfig)
                          (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Either[(ErrorDTO, StatusCode), Unit]] =
    if (reCaptchaConfig.checkOn)
      captchaCheckDirective1(header, log, reCaptchaConfig)
    else
      Future.successful(().asRight)

  protected def captchaCheckDirective(userCaptcha: String,
                                      log: LoggingAdapter,
                                      reCaptchaConfig: CaptchaConfig)
                                     (implicit actorSystem: ActorSystem): Directive[Unit] = Directive[Unit] {
    inner => ctx =>

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


  protected def captchaCheckDirective1(userCaptcha: String,
                                      log: LoggingAdapter,
                                      reCaptchaConfig: CaptchaConfig)
                                     (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Either[(Error, StatusCode), Unit]] = {

      implicit val materializer: ActorMaterializer = ActorMaterializer()

      for {
        verifyResponse <- doRequestToVerifyCaptcha1(userCaptcha, log, reCaptchaConfig)
        result <- verifyResponse match {
          case Right(response) => checkVerifyResponse1(response, log)
          case Left(error) => Future.successful((error, StatusCode.BadRequest).asLeft)
        }
      } yield result
  }

  protected def doRequestToVerifyCaptcha1(userCaptcha: String,
                                         log: LoggingAdapter,
                                         reCaptchaConfig: CaptchaConfig
                                        )
                                        (implicit actorSystem: ActorSystem,
                                         ec: ExecutionContext): Future[Either[Error, HttpResponse]] = {
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
          Left(Error("Can't do request to verify captcha"))
      }
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
          Left(complete(StatusCodes.InternalServerError, Error("Can't do request to verify captcha")))
      }
  }

  protected def checkVerifyResponse1(response: HttpResponse,
                                    log: LoggingAdapter)
                                   (implicit am: ActorMaterializer, ec: ExecutionContext): Future[Either[(Error, StatusCode), Unit]] = {

    val dateFormatter = DateTimeFormat.forPattern("yyyyMMdd")
    implicit val decodeDateTime: Decoder[DateTime] = Decoder.decodeString.emap { s =>
      try {
        Right(DateTime.parse(s, dateFormatter))
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }
    }

    val unmarshalResult = Unmarshal(response).to[CaptchaVerifyResponse]
    unmarshalResult.map { value =>
      if (value.success)
        Right(())
      else
        (Error("Invalid captcha"), StatusCode.Unauthorized).asLeft
    }.recover {
      case err =>
        log.error(s"Can't parse google reCaptcha response, err - $err")
        (Error("Can't parse captcha response from google"), StatusCode.InternalServerError).asLeft
    }
  }

  protected def checkVerifyResponse(response: HttpResponse,
                                    log: LoggingAdapter,
                                    inner: Unit => Route)
                                   (implicit am: ActorMaterializer, ec: ExecutionContext): Future[Route] = {

    val dateFormatter = DateTimeFormat.forPattern("yyyyMMdd")
    implicit val decodeDateTime: Decoder[DateTime] = Decoder.decodeString.emap { s =>
      try {
        Right(DateTime.parse(s, dateFormatter))
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }
    }

    val unmarshalResult = Unmarshal(response).to[CaptchaVerifyResponse]
    unmarshalResult.map { value =>
      if (value.success)
        inner(())
      else
        complete(StatusCodes.Unauthorized, Error("Invalid captcha"))
    }.recover {
      case err =>
        log.error(s"Can't parse google reCaptcha response, err - $err")
        complete(StatusCodes.InternalServerError, Error("Can't parse captcha response from google"))
    }
  }

}
