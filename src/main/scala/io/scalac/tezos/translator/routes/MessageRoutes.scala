package io.scalac.tezos.translator.routes
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Route}
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.{Error, SendEmailDTO}
import io.scalac.tezos.translator.routes.util.ReCaptchaDirective._
import io.scalac.tezos.translator.service.Emails2SendService
import util.DTOValidationDirective._

import scala.util.{Failure, Success}

class MessageRoutes(service: Emails2SendService,
                    log: LoggingAdapter,
                    reCaptchaConfig: CaptchaConfig)
                   (implicit actorSystem: ActorSystem) extends HttpRoutes with JsonHelper {

  override def routes: Route =
    (path ("message") & pathEndOrSingleSlash & withReCaptchaVerify(log, reCaptchaConfig)(actorSystem)
      & withSendMessageValidation) { sendEmail =>
      val operationPerformed = service.addNewEmail2Send(sendEmail)
      onComplete(operationPerformed) {
        case Success(_)   => complete(StatusCodes.OK)
        case Failure(err) =>
          log.error(s"Can't add email to send, err - $err")
          complete(StatusCodes.InternalServerError, Error("Can't save payload"))
      }
    }

  def withSendMessageValidation: Directive[Tuple1[SendEmailDTO]] = withDTOValidation[SendEmailDTO]

}
