package io.scalac.tezos.translator.routes

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.LibraryEntry.{Accepted, PendingApproval, Status}
import io.scalac.tezos.translator.model.{AuthUserData, EmailAddress, SendEmail}
import io.scalac.tezos.translator.routes.dto.DTOValidation
import io.scalac.tezos.translator.routes.dto.DTO.Error
import io.scalac.tezos.translator.routes.utils.ReCaptcha._
import io.scalac.tezos.translator.routes.dto.{DTO, LibraryEntryDTO, LibraryEntryRoutesAdminDto, LibraryEntryRoutesDto}
import io.scalac.tezos.translator.routes.dto.LibraryEntryDTO._
import io.scalac.tezos.translator.model.types.UUIDs.LibraryEntryId
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, UserService}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._
import cats.syntax.either._
import io.scalac.tezos.translator.routes.Endpoints._
import io.scalac.tezos.translator.model.types.Auth.{Captcha, UserToken}
import io.scalac.tezos.translator.model.types.Params._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class LibraryRoutes(
  service: LibraryService,
  userService: UserService,
  emails2SendService: Emails2SendService,
  log: LoggingAdapter,
  captchaConfig: CaptchaConfig,
  adminEmail: EmailAddress
)(implicit as: ActorSystem, ec: ExecutionContext) extends HttpRoutes {
  import io.circe.generic.auto._

  private val libraryCaptchaEndpoint: Endpoint[Option[Captcha], ErrorResponse, Unit, Nothing] = Endpoints.captchaEndpoint(captchaConfig).in("library")
  private  val libraryEndpoint: Endpoint[Unit, Unit, Unit, Nothing] = Endpoints.baseEndpoint.in("library")

  private val libraryAddEndpoint: Endpoint[(Option[Captcha], LibraryEntryRoutesDto), ErrorResponse, StatusCode, Nothing] =
    libraryCaptchaEndpoint
      .in(jsonBody[LibraryEntryRoutesDto])
      .post
      .out(statusCode)
      .description("Will add LibraryEntryRoutesDto to storage")

  private val getDtoEndpoint: Endpoint[(Option[UserToken], Option[Offset], Option[Limit]), ErrorResponse, Seq[LibraryEntryDTO], Nothing] =
    libraryEndpoint
      .in(maybeAuthHeader)
      .in(offsetQuery.and(limitQuery))
      .errorOut(errorResponse)
      .out(jsonBody[Seq[LibraryEntryDTO]])
      .get
      .description("Will return sequence of LibraryEntryRoutesDto or LibraryEntryRoutesAdminDto if auth header provided")

  private val putEntryEndpoint: Endpoint[(String, String, String), ErrorResponse, StatusCode, Nothing] =
    libraryEndpoint
      .in(auth.bearer)
      .in(uidQuery.and(statusQuery))
      .errorOut(errorResponse)
      .out(statusCode)
      .put
      .description("Will change the status of entry with passed uid")

  private val deleteEntryEndpoint: Endpoint[(String, String), ErrorResponse, StatusCode, Nothing] =
    libraryEndpoint
      .in(auth.bearer)
      .in(uidQuery)
      .errorOut(errorResponse)
      .out(statusCode)
      .delete
      .description("Will delete an entry with passed uid")

  private def addNewEntryRoute(): Route =
    libraryAddEndpoint.toRoute {
      (withReCaptchaVerify(_, log, captchaConfig))
        .andThenFirstE { t: (Unit, LibraryEntryRoutesDto) => DTOValidation.validateDto(t._2) }
        .andThenFirstE(addNewEntry)
    }

  private def getDTORoute: Route =
    getDtoEndpoint.toRoute {
      (getDto _).tupled
    }

  private def putEntryRoute: Route =
    putEntryEndpoint.toRoute {
      (bearer2TokenF _).andThenFirstE(userService.authenticate).andThenFirstE((putDto _).tupled)
    }

  private def deleteEntryRoute: Route =
    deleteEntryEndpoint.toRoute {
      (bearer2TokenF _).andThenFirstE(userService.authenticate).andThenFirstE((deleteDto _).tupled)
    }

  private def addNewEntry(libraryDTO: LibraryEntryRoutesDto): Future[Either[ErrorResponse, StatusCode]] = {
    val sendEmailF = libraryDTO.email match {
      case Some(_) =>
        val e = SendEmail.approvalRequest(libraryDTO, adminEmail)
        emails2SendService
          .addNewEmail2Send(e)
          .recover { case err => log.error(s"Can't add new email to send, error - $err") }

      case None => Future.successful(())
    }

    val operationPerformed = for {
      entry       <-  Future.fromTry(libraryDTO.toDomain)
      addResult   <-  service.addNew(entry)
      _           <-  sendEmailF
    } yield addResult
    operationPerformed.map(_ => StatusCode.Ok.asRight).recover { case e =>
      log.error(s"Can't save library dto $libraryDTO, error - $e")
      (Error("Can't save payload"), StatusCode.InternalServerError).asLeft
    }
  }

  private def getAdminsDto(maybeOffset: Option[Offset],
                           maybeLimit:  Option[Limit]): Future[Either[ErrorResponse, Seq[LibraryEntryDTO]]] = {
    service
      .getRecords(maybeOffset, maybeLimit)
      .map(_.map(LibraryEntryRoutesAdminDto.fromDomain).asRight)
      .recover { case e =>
        log.error(s"Can't show accepted library models, error - $e")
        (Error("Can't get records"), StatusCode.InternalServerError).asLeft
      }
  }

  private def getDto(maybeToken:  Option[UserToken],
                     maybeOffset: Option[Offset],
                     maybeLimit:  Option[Limit]): Future[Either[ErrorResponse, Seq[LibraryEntryDTO]]] =
    maybeToken
      .withMaybeAuth(userService)(getAdminsDto(maybeOffset, maybeLimit))(getJustDto(maybeOffset, maybeLimit))
      .value

  private def getJustDto(maybeOffset: Option[Offset],
                         maybeLimit:  Option[Limit]): Future[Either[ErrorResponse, Seq[LibraryEntryDTO]]] =
    service
      .getRecords(maybeOffset, maybeLimit, Some(Accepted))
      .map(_.map(LibraryEntryRoutesDto.fromDomain).asRight)
    .recover { case e =>
      log.error(s"Can't show accepted library models, limit - $maybeLimit error - $e")
      (Error("Can't get records"), StatusCode.InternalServerError).asLeft
    }

  private def putDto(userData: AuthUserData,
                     uid: String,
                     status: String): Future[Either[ErrorResponse, StatusCode]] = {
    val statusChangeWithEmail =
      for {
        s             <-  Future.fromTry(Status.fromString(status) match {
          case Success(PendingApproval) =>
            Failure(new IllegalArgumentException("Cannot change status to 'pending_approval' !"))
          case other => other
        })
        u             =  LibraryEntryId(UUID.fromString(uid))
        updatedEntry  <- service.changeStatus(u, s)
        _             <- updatedEntry.email match {
          case Some(email) =>
            val e = SendEmail.statusChange(email, updatedEntry.title , s)
            emails2SendService
              .addNewEmail2Send(e)
              .recover { case err => log.error(s"Can't add new email to send, error - $err") }
          case None => Future.successful(())
        }
      } yield updatedEntry
    statusChangeWithEmail.map(_ => StatusCode.Ok.asRight).recover { case e =>
      log.error(s"Cannot update library entry, uid: $uid, error - $e")
      handleError(e).asLeft
    }
  }

  private def deleteDto(userData: AuthUserData, uid: String): Future[Either[ErrorResponse, StatusCode]] =
    service.delete(LibraryEntryId(UUID.fromString(uid)))
      .map(_ => StatusCode.Ok.asRight)
      .recover { case e =>
        log.error(s"Can't delete library entry, uid: $uid, error - $e")
        handleError(e).asLeft
      }

  private def handleError(t: Throwable): ErrorResponse =
    t match {
      case _: IllegalArgumentException => (Error(t.getMessage), StatusCode.NotFound)
      case _ => (Error("Can't update"), StatusCode.InternalServerError)
    }

  override def routes: Route = addNewEntryRoute ~ getDTORoute ~ putEntryRoute ~ deleteEntryRoute

  override def docs: List[Endpoint[_, _ ,_ ,_]] =
    List(libraryAddEndpoint, getDtoEndpoint, putEntryEndpoint, deleteEntryEndpoint)

}
