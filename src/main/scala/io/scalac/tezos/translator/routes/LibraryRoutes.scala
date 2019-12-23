package io.scalac.tezos.translator.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.LibraryEntry.{Accepted, PendingApproval, Status}
import io.scalac.tezos.translator.model.{EmailAddress, SendEmail, Uid}
import io.scalac.tezos.translator.routes.directives.DTOValidationDirective
import io.scalac.tezos.translator.routes.dto.DTO.{Error, ErrorDTO}
import io.scalac.tezos.translator.routes.directives.ReCaptchaDirective._
import io.scalac.tezos.translator.routes.dto.{LibraryEntryRoutesAdminDto, LibraryEntryRoutesDto}
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, UserService}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._
import cats.syntax.either._
import io.scalac.tezos.translator.routes.Endpoints._

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
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val libraryCaptchaEndpoint: Endpoint[String, ErrorResponse, Unit, Nothing] = Endpoints.captchaEndpoint(captchaConfig).in("library")
  private  val libraryEndpoint: Endpoint[Unit, Unit, Unit, Nothing] = Endpoints.baseEndpoint.in("library")

  private val libraryAddEndpoint: Endpoint[(String, LibraryEntryRoutesDto), ErrorResponse, StatusCode, Nothing] =
    libraryCaptchaEndpoint
      .in(jsonBody[LibraryEntryRoutesDto])
      .post
      .out(statusCode)
      .description("Will add LibraryEntryRoutesDto to storage")

  private val getDtoEndpoint: Endpoint[(Option[Int], Option[Int]), ErrorResponse, Seq[LibraryEntryRoutesDto], Nothing] =
    libraryEndpoint
      .in(offsetQuery.and(limitQuery))
      .errorOut(errorResponse)
      .out(jsonBody[Seq[LibraryEntryRoutesDto]])
      .get
      .description("Will return sequence of LibraryEntryRoutesDto")

  private val getAdminsDtoEndpoint: Endpoint[(String, Option[Int], Option[Int]), ErrorResponse, Seq[LibraryEntryRoutesAdminDto], Nothing] =
    libraryEndpoint
      .in(auth.bearer)
      .in(offsetQuery.and(limitQuery))
      .errorOut(errorResponse)
      .out(jsonBody[Seq[LibraryEntryRoutesAdminDto]])
      .get
      .description("Will return sequence of LibraryEntryRoutesAdminDto")

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
      (withReCaptchaVerify1(_, log, captchaConfig))
        .andThenFirstE((validateLibraryEntryRoutesDTO _).tupled)
        .andThenFirstE(addNewEntry)
    }

  private def getDTORoute: Route =
    getDtoEndpoint.toRoute {
      (getDto _).tupled
    }

  private def getAdminsDTORoute: Route =
    getAdminsDtoEndpoint.toRoute {
      (userService.authenticateOAuth2AndPrependUsername1 _).andThenFirstE((getAdminsDto _).tupled)
    }

  private def putEntryRoute: Route =
    putEntryEndpoint.toRoute {
      (userService.authenticateOAuth2AndPrependUsername1 _).andThenFirstE((putDto _).tupled)
    }

  private def deleteEntryRoute: Route =
    deleteEntryEndpoint.toRoute {
      (userService.authenticateOAuth2AndPrependUsername1 _).andThenFirstE((deleteDto _).tupled)
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

  private def getAdminsDto(userAndToken: (String, String),
                           maybeOffset: Option[Int],
                           maybeLimit: Option[Int]): Future[Either[ErrorResponse, Seq[LibraryEntryRoutesAdminDto]]] = {
    service
      .getRecords(maybeOffset, maybeLimit)
      .map(_.map(LibraryEntryRoutesAdminDto.fromDomain).asRight)
      .recover { case e =>
        log.error(s"Can't show accepted library models, error - $e")
        (Error("Can't get records"), StatusCode.InternalServerError).asLeft
      }
  }

  private def getDto(maybeOffset: Option[Int],
                     maybeLimit: Option[Int]): Future[Either[ErrorResponse, Seq[LibraryEntryRoutesDto]]] =
    service
      .getRecords(maybeOffset, maybeLimit, Some(Accepted))
      .map(_.map(LibraryEntryRoutesDto.fromDomain).asRight)
    .recover { case e =>
      log.error(s"Can't show accepted library models, limit - $maybeLimit error - $e")
      (Error("Can't get records"), StatusCode.InternalServerError).asLeft
    }

  private def putDto(userData: (String, String),
                     uid: String,
                     status: String): Future[Either[ErrorResponse, StatusCode]] = {
    val statusChangeWithEmail =
      for {
        u             <-  Future.fromTry(Uid.fromString(uid))
        parsedStatus  =   Status.fromString(status) match {
          case Success(PendingApproval) =>
            Failure(new IllegalArgumentException("Cannot change status to 'pending_approval' !"))
          case other => other
        }
        s             <-  Future.fromTry(parsedStatus)
        updatedEntry  <-  service.changeStatus(u, s)
        _             <-  updatedEntry.email match {
          case Some(email) =>
            val e = SendEmail.statusChange(email, updatedEntry.name , s)
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

  private def deleteDto(userData: (String, String), uid: String): Future[Either[ErrorResponse, StatusCode]] =
    Future
      .fromTry(Uid.fromString(uid).map(service.delete))
      .flatten
      .map(_ => StatusCode.Ok.asRight)
      .recover { case e =>
        log.error(s"Can't delete library entry, uid: $uid, error - $e")
        handleError(e).asLeft
      }

  private def validateLibraryEntryRoutesDTO(x: Unit,
                                            LibraryEntryRoutesDTO: LibraryEntryRoutesDto)
                                           (implicit ec: ExecutionContext): Future[Either[ErrorResponse, LibraryEntryRoutesDto]] =
    DTOValidationDirective.validateDto(LibraryEntryRoutesDTO)

  private def handleError(t: Throwable): ErrorResponse =
    t match {
      case _: IllegalArgumentException => (Error(t.getMessage), StatusCode.NotFound)
      case _ => (Error("Can't update"), StatusCode.InternalServerError)
    }

  override def routes: Route = addNewEntryRoute ~ getDTORoute ~ getAdminsDTORoute ~ putEntryRoute ~ deleteEntryRoute

  override def docs: List[Endpoint[_, _ ,_ ,_]] =
    List(libraryAddEndpoint, getDtoEndpoint, getAdminsDtoEndpoint, putEntryEndpoint, deleteEntryEndpoint)

}
