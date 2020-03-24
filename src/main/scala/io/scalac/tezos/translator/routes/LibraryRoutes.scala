package io.scalac.tezos.translator.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route
import cats.syntax.either._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.LibraryEntry.{ Accepted, PendingApproval, Status }
import io.scalac.tezos.translator.model.types.Auth.{ Captcha, UserToken }
import io.scalac.tezos.translator.model.types.Params._
import io.scalac.tezos.translator.model.types.UUIDs.{ LibraryEntryId, UUIDString }
import io.scalac.tezos.translator.model.{ AuthUserData, EmailAddress, SendEmail }
import io.scalac.tezos.translator.routes.Endpoints._
import io.scalac.tezos.translator.routes.dto.DTO.Error
import io.scalac.tezos.translator.routes.dto.LibraryEntryDTO._
import io.scalac.tezos.translator.routes.dto._
import io.scalac.tezos.translator.routes.utils.ReCaptcha._
import io.scalac.tezos.translator.service.{ Emails2SendService, LibraryService, UserService }
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class LibraryRoutes(
   service: LibraryService,
   userService: UserService,
   emails2SendService: Emails2SendService,
   log: LoggingAdapter,
   captchaConfig: CaptchaConfig,
   adminEmail: EmailAddress
 )(implicit as: ActorSystem,
   ec: ExecutionContext)
    extends HttpRoutes {
  import io.circe.generic.auto._

  private val libraryCaptchaEndpoint: Endpoint[Option[Captcha], ErrorResponse, Unit, Nothing] =
    Endpoints.captchaEndpoint(captchaConfig).in("library")
  private val libraryEndpoint             = Endpoints.baseEndpoint.in("library")
  private val libraryEntryEndpoint        = libraryEndpoint.in(uidPath)
  private val librarySecuredEntryEndpoint = libraryEndpoint.in(auth.bearer).in(uidPath)

  private def handleError(t: Throwable): ErrorResponse =
    t match {
      case _: IllegalArgumentException => (Error(t.getMessage), StatusCode.NotFound)
      case _                           => (Error("Can't update"), StatusCode.InternalServerError)
    }

  /** Endpoints **/
  override def docs = List(addEntryEndpoint, getAllEntriesEndpoint, getSingleEntryEndpoint, putEntryEndpoint, deleteEntryEndpoint)

  private val addEntryEndpoint: Endpoint[(Option[Captcha], LibraryEntryRoutesNewDto), ErrorResponse, StatusCode, Nothing] =
    libraryCaptchaEndpoint
      .in(jsonBody[LibraryEntryRoutesNewDto])
      .post
      .out(statusCode)
      .description("Will add LibraryEntryRoutesNewDto to storage")

  private val getAllEntriesEndpoint
     : Endpoint[(Option[UserToken], Option[Offset], Option[Limit]), ErrorResponse, Seq[LibraryEntryDTO], Nothing] =
    libraryEndpoint
      .in(maybeAuthHeader)
      .in(offsetQuery.and(limitQuery))
      .errorOut(errorResponse)
      .out(jsonBody[Seq[LibraryEntryDTO]])
      .get
      .description("Will return sequence of LibraryEntryRoutesDto or LibraryEntryRoutesAdminDto if auth header provided")

  private val getSingleEntryEndpoint: Endpoint[(UUIDString, Option[UserToken]), ErrorResponse, LibraryEntryDTO, Nothing] =
    libraryEntryEndpoint
      .in(maybeAuthHeader)
      .errorOut(errorResponse)
      .out(jsonBody[LibraryEntryDTO])
      .get
      .description("Will return LibraryEntryRoutesDto with given UUID or LibraryEntryRoutesAdminDto if auth header provided")

  private val putEntryEndpoint: Endpoint[(String, UUIDString, String), ErrorResponse, StatusCode, Nothing] =
    librarySecuredEntryEndpoint
      .in(statusQuery)
      .errorOut(errorResponse)
      .out(statusCode)
      .put
      .description("Will change the status of entry with passed UUID")

  private val deleteEntryEndpoint: Endpoint[(String, UUIDString), ErrorResponse, StatusCode, Nothing] =
    librarySecuredEntryEndpoint
      .errorOut(errorResponse)
      .out(statusCode)
      .delete
      .description("Will delete an entry with passed UUID")

  /** Routes **/
  override def routes = addEntryRoute ~ getSingleEntryRoute ~ getAllEntriesRoute ~ putEntryRoute ~ deleteEntryRoute

  /** Routes :: Add entry **/

  private def addEntryRoute: Route =
    addEntryEndpoint.toRoute {
      (withReCaptchaVerify(_, log, captchaConfig))
        .andThenFirstE { t: (Unit, LibraryEntryRoutesNewDto) => DTOValidation.validateDto(t._2) }
        .andThenFirstE(addEntry)
    }

  private def addEntry(newEntry: LibraryEntryRoutesNewDto): Future[Either[ErrorResponse, StatusCode]] = {
    val sendEmailF = newEntry.email match {
      case Some(_) =>
        val e = SendEmail.approvalRequest(newEntry, adminEmail)
        emails2SendService
          .addNewEmail2Send(e)
          .recover {
            case err =>
              log.error(s"Can't add new email to send, error - $err")
              1
          }

      case None => Future.successful(())
    }

    val operationPerformed = for {
      entry     <- Future.fromTry(newEntry.toDomain)
      addResult <- service.addNew(entry)
      _         <- sendEmailF
    } yield addResult
    operationPerformed.map(_ => StatusCode.Ok.asRight).recover {
      case e =>
        log.error(s"Can't save library dto $newEntry, error - $e")
        (Error("Can't save payload"), StatusCode.InternalServerError).asLeft
    }
  }

  /** Routes :: Get all entries **/

  private def getAllEntriesRoute: Route =
    getAllEntriesEndpoint.toRoute {
      (getAllEntries _).tupled
    }

  private def getAllEntries(
     maybeToken: Option[UserToken],
     maybeOffset: Option[Offset],
     maybeLimit: Option[Limit]
   ): Future[Either[ErrorResponse, Seq[LibraryEntryDTO]]] =
    maybeToken
      .withMaybeAuth(userService)(getEntriesForAdmin(maybeOffset, maybeLimit))(getEntries(maybeOffset, maybeLimit))
      .value

  private def getEntriesForAdmin(
     maybeOffset: Option[Offset],
     maybeLimit: Option[Limit]
   ): Future[Either[ErrorResponse, Seq[LibraryEntryDTO]]] =
    service
      .getEntries(maybeOffset, maybeLimit)
      .map(_.map(LibraryEntryRoutesAdminDto.fromDomain).asRight)
      .recover {
        case e =>
          log.error(s"Can't show accepted library models, error - $e")
          (Error("Can't get records"), StatusCode.InternalServerError).asLeft
      }

  private def getEntries(maybeOffset: Option[Offset], maybeLimit: Option[Limit]): Future[Either[ErrorResponse, Seq[LibraryEntryDTO]]] =
    service
      .getEntries(maybeOffset, maybeLimit, Some(Accepted))
      .map(_.map(LibraryEntryRoutesDto.fromDomain).asRight)
      .recover {
        case e =>
          log.error(s"Can't show accepted library models, limit - $maybeLimit error - $e")
          (Error("Can't get records"), StatusCode.InternalServerError).asLeft
      }

  /** Routes :: Get single entry **/

  private def getSingleEntryRoute: Route =
    getSingleEntryEndpoint.toRoute {
      (getSingleEntry _).tupled
    }

  private def getSingleEntry(uid: UUIDString, maybeToken: Option[UserToken]): Future[Either[ErrorResponse, LibraryEntryDTO]] =
    maybeToken
      .withMaybeAuth(userService)(getEntryForAdmin(uid))(getEntry(uid))
      .value

  private def getEntryForAdmin(uid: UUIDString): Future[Either[ErrorResponse, LibraryEntryDTO]] =
    service
      .getEntry(LibraryEntryId(uid))
      .map(LibraryEntryRoutesAdminDto.fromDomain)
      .map(_.asRight)
      .recover {
        case e =>
          log.error(s"Can't show accepted library model, uid: $uid, error - $e")
          handleError(e).asLeft
      }

  private def getEntry(uid: UUIDString): Future[Either[ErrorResponse, LibraryEntryDTO]] =
    service
      .getEntryWithOptionalFilter(LibraryEntryId(uid), Some(Accepted))
      .map(LibraryEntryRoutesDto.fromDomain)
      .map(_.asRight)
      .recover {
        case e =>
          log.error(s"Can't show accepted library model, uid: $uid, error - $e")
          handleError(e).asLeft
      }

  /** Routes :: Put entry **/

  private def putEntryRoute: Route =
    putEntryEndpoint.toRoute {
      (bearer2TokenF _)
        .andThenFirstE(userService.authenticate)
        .andThenFirstE { args: (AuthUserData, UUIDString, String) => putEntry(args._2, args._3) }
    }

  private def putEntry(uid: UUIDString, status: String): Future[Either[ErrorResponse, StatusCode]] = {
    val statusChangeWithEmail =
      for {
        s <- Future.fromTry(Status.fromString(status) match {
              case Success(PendingApproval) =>
                Failure(new IllegalArgumentException("Cannot change status to 'pending_approval'!"))
              case other => other
            })
        u = LibraryEntryId(uid)

        updatedEntry <- service.changeStatus(u, s)
        _ <- updatedEntry.email match {
              case Some(email) =>
                val e = SendEmail.statusChange(email, updatedEntry.title, s)
                emails2SendService
                  .addNewEmail2Send(e)
                  .recover { case err => log.error(s"Can't add new email to send, error - $err") }
              case None => Future.successful(())
            }
      } yield updatedEntry
    statusChangeWithEmail.map(_ => StatusCode.Ok.asRight).recover {
      case e =>
        log.error(s"Cannot update library entry, uid: $uid, error - $e")
        handleError(e).asLeft
    }
  }

  /** Routes :: Delete entry **/

  private def deleteEntryRoute: Route =
    deleteEntryEndpoint.toRoute {
      (bearer2TokenF _)
        .andThenFirstE(userService.authenticate)
        .andThenFirstE { args: (AuthUserData, UUIDString) => deleteEntry(args._2) }
    }

  private def deleteEntry(uid: UUIDString): Future[Either[ErrorResponse, StatusCode]] =
    service
      .delete(LibraryEntryId(uid))
      .map(_ => StatusCode.Ok.asRight)
      .recover {
        case e =>
          log.error(s"Can't delete library entry, uid: $uid, error - $e")
          handleError(e).asLeft
      }

}
