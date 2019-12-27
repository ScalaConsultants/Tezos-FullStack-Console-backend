package io.scalac.tezos.translator.routes
import akka.actor.ActorSystem
import io.scalac.tezos.translator.model.{EmailAddress, SendEmail}
import io.scalac.tezos.translator.routes.utils.ReCaptcha
import io.scalac.tezos.translator.routes.dto.DTO.Error
import io.scalac.tezos.translator.routes.dto.DTOValidation
import io.scalac.tezos.translator.routes.dto.{DTO, SendEmailRoutesDto}
import io.scalac.tezos.translator.service.Emails2SendService
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route
import cats.syntax.either._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.routes.Endpoints.ErrorResponse
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

  private val messageEndpoint: Endpoint[(Option[String], SendEmailRoutesDto), (DTO.ErrorDTO, StatusCode), StatusCode, Nothing] =
    Endpoints
      .captchaEndpoint(reCaptchaConfig)
      .post
      .in("message")
      .in(jsonBody[SendEmailRoutesDto])
      .out(statusCode)

  private def addNewEmail(dto: SendEmailRoutesDto): Future[Either[ErrorResponse, StatusCode]] = {
    val operationPerformed = for {
        sendEmail <-  Future.fromTry(SendEmail.fromSendEmailRoutesDto(dto, adminEmail))
        newEmail  <-  service.addNewEmail2Send(sendEmail)
      } yield newEmail

    operationPerformed.map(_ => StatusCode.Ok.asRight).recover {
      case e =>
        log.error(s"Can't add email to send, err - $e")
        (Error("Can't save payload"), StatusCode.InternalServerError).asLeft
    }
  }

  private def validateSendMessage(x: Unit,
                                  sendEmailRoutesDto: SendEmailRoutesDto)
                                 (implicit ec: ExecutionContext): Future[Either[ErrorResponse, SendEmailRoutesDto]] =
    DTOValidation.validateDto(sendEmailRoutesDto)

  def buildRoute(log: LoggingAdapter, reCaptchaConfig: CaptchaConfig)(implicit ec: ExecutionContext): Route =
    messageEndpoint
      .toRoute {
        (ReCaptcha.withReCaptchaVerify(_, log, reCaptchaConfig))
          .andThenFirstE((validateSendMessage _).tupled)
          .andThenFirstE(addNewEmail)
      }

  override def routes: Route = buildRoute(log, reCaptchaConfig)

  override def docs: List[Endpoint[_, _, _, _]] = List(messageEndpoint)

}
