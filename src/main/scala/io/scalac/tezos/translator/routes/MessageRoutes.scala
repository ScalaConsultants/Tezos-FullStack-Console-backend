package io.scalac.tezos.translator.routes
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directive
import io.scalac.tezos.translator.model.{EmailAddress, SendEmail}
import io.scalac.tezos.translator.routes.directives.{DTOValidationDirective, ReCaptchaDirective}
import io.scalac.tezos.translator.routes.dto.DTO.{Error, ErrorDTO}
import io.scalac.tezos.translator.routes.directives.DTOValidationDirective._
import io.scalac.tezos.translator.routes.dto.SendEmailRoutesDto
import io.scalac.tezos.translator.service.Emails2SendService
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route
import cats.syntax.either._
import io.scalac.tezos.translator.config.CaptchaConfig
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._
import scala.concurrent.{ExecutionContext, Future}

class MessageRoutes(
  service: Emails2SendService,
  log: LoggingAdapter,
  reCaptchaConfig: CaptchaConfig,
  adminEmail: EmailAddress
)(implicit actorSystem: ActorSystem, ec: ExecutionContext) extends HttpRoutes {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  override def routes: Route = buildRoute(log, reCaptchaConfig)

  def addNewEmail(dto: SendEmailRoutesDto): Future[Either[(Error, StatusCode), StatusCode]] =
    service
      .addNewEmail2Send(SendEmail.fromSendEmailRoutesDto(dto, adminEmail))
      .map(_ => StatusCode.Ok.asRight)
    .recover {
      case err =>
        log.error(s"Can't add email to send, err - $err")
        (Error("Can't save payload"), StatusCode.InternalServerError).asLeft
    }

  def withSendMessageValidation: Directive[Tuple1[SendEmailRoutesDto]] = withDTOValidation[SendEmailRoutesDto]

  def validateSendMessage(x: Unit,
                          sendEmailRoutesDto: SendEmailRoutesDto)
                         (implicit ec: ExecutionContext): Future[Either[(ErrorDTO, StatusCode), SendEmailRoutesDto]] =
    DTOValidationDirective.withDTOValidation1(sendEmailRoutesDto)

  def buildRoute(log: LoggingAdapter, reCaptchaConfig: CaptchaConfig)(implicit ec: ExecutionContext): Route =
    ReCaptchaDirective.captchaEndpoint(reCaptchaConfig).in(jsonBody[SendEmailRoutesDto]).in("message").out(statusCode).toRoute {
      (ReCaptchaDirective.withReCaptchaVerify1(_, log, reCaptchaConfig)).andThenFirstE((validateSendMessage _).tupled).andThenFirstE(addNewEmail)
    }

}
