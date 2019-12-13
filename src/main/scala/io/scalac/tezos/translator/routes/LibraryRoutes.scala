package io.scalac.tezos.translator.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Route, StandardRoute}
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.LibraryEntry.{Accepted, PendingApproval, Status}
import io.scalac.tezos.translator.model.{EmailAddress, Error, SendEmail, Uid}
import io.scalac.tezos.translator.routes.directives.DTOValidationDirective._
import io.scalac.tezos.translator.routes.directives.ReCaptchaDirective._
import io.scalac.tezos.translator.routes.dto.{LibraryEntryRoutesAdminDto, LibraryEntryRoutesDto}
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, UserService}

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

  override def routes: Route =
    (path("library") & pathEndOrSingleSlash) {
      (post & withReCaptchaVerify(log, captchaConfig)(as) & withLibraryDTOValidation) { libraryDto =>
        val sendEmailF = libraryDto.email match {
          case Some(_) =>
            val e = SendEmail.approvalRequest(libraryDto, adminEmail)
            emails2SendService
              .addNewEmail2Send(e)
              .recover { case err => log.error(s"Can't add new email to send, error - $err") }

          case None => Future.successful(())
        }

        val operationPerformed = for {
          entry       <-  Future.fromTry(libraryDto.toDomain)
          addResult   <-  service.addNew(entry)
          _           <-  sendEmailF
        } yield addResult

        onComplete(operationPerformed) {
          case Success(_) => complete(StatusCodes.OK)
          case Failure(err) =>
            log.error(s"Can't save library dto $libraryDto, error - $err")
            complete(StatusCodes.InternalServerError, Error("Can't save payload"))
        }
      } ~
        (get
          & authenticateOAuth2("", userService.authenticateOAuth2AndPrependUsername)
          & parameters('offset.as[Int].?, 'limit.as[Int].?)) { case (_, offset, limit) =>
          val operationPerformed = service.getRecords(offset, limit)
          onComplete(operationPerformed) {
            case Success(libraryEntries) => complete(libraryEntries.map(LibraryEntryRoutesAdminDto.fromDomain))
            case Failure(err) =>
              log.error(s"Can't show accepted library models, error - $err")
              complete(StatusCodes.InternalServerError, Error("Can't get records"))
          }
        } ~
        (get & parameters('offset.as[Int].?, 'limit.as[Int].?)) { case (offset, limit) =>
          val operationPerformed = service.getRecords(offset, limit, Some(Accepted))
          onComplete(operationPerformed) {
            case Success(libraryEntries) => complete(libraryEntries.map(LibraryEntryRoutesDto.fromDomain))
            case Failure(err) =>
              log.error(s"Can't show accepted library models, limit - $limit error - $err")
              complete(StatusCodes.InternalServerError, Error("Can't get records"))
          }
        } ~
        (authenticateOAuth2("", userService.authenticateOAuth2AndPrependUsername) &
          put &
          parameters('uid.as[String], 'status.as[String])
          ) { case (_, uid, status) =>

            val statusChangeWithEmail =
              for {
                u             <-  Future.fromTry(Uid.fromString(uid))
                parsedStatus  =   Status.fromString(status) match {
                                    case Success(PendingApproval) => Failure(new IllegalArgumentException("Cannot change status to 'pending_approval' !"))
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

            onComplete(statusChangeWithEmail) {
              case Success(_) =>
                complete(StatusCodes.OK)

              case Failure(err) =>
                log.error(s"Cannot update library entry, uid: $uid, error - $err")
                handleError(err)
            }
        } ~
        (authenticateOAuth2("", userService.authenticateOAuth2AndPrependUsername) &
          delete &
          parameters('uid.as[String])
          ) { case (_, uid) =>
              onComplete(Future.fromTry(Uid.fromString(uid).map(service.delete)).flatten) {
                case Success(_) =>
                  complete(StatusCodes.OK)

                case Failure(err) =>
                  log.error(s"Can't delete library entry, uid: $uid, error - $err")
                  handleError(err)
              }
            }
    }

  def withLibraryDTOValidation: Directive[Tuple1[LibraryEntryRoutesDto]] = withDTOValidation[LibraryEntryRoutesDto]

  private def handleError(t: Throwable): StandardRoute =
    t match {
      case _: IllegalArgumentException => complete(StatusCodes.NotFound, Error(t.getMessage))
      case _ => complete(StatusCodes.InternalServerError, Error("Can't update"))
    }

}
