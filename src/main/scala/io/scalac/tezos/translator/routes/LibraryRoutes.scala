package io.scalac.tezos.translator.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Route, StandardRoute}
import io.scalac.tezos.translator.config.Configuration
import io.scalac.tezos.translator.model.LibraryEntry.{Accepted, Status}
import io.scalac.tezos.translator.model.{Error, Uid}
import io.scalac.tezos.translator.routes.directives.DTOValidationDirective._
import io.scalac.tezos.translator.routes.directives.ReCaptchaDirective._
import io.scalac.tezos.translator.routes.dto.LibraryEntryRoutesDto
import io.scalac.tezos.translator.service.LibraryService.UidNotExists
import io.scalac.tezos.translator.service.{LibraryService, UserService}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class LibraryRoutes(
  service: LibraryService,
  userService: UserService,
  log: LoggingAdapter,
  config: Configuration
)(implicit as: ActorSystem) extends HttpRoutes with JsonHelper {
  override def routes: Route =
    (path("library") & pathEndOrSingleSlash) {
      (post & withReCaptchaVerify(log, config.reCaptcha)(as) & withLibraryDTOValidation) { libraryDto =>
        val operationPerformed = service.addNew(libraryDto.toDomain)
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
            case Success(libraryEntries) => complete(libraryEntries.map(LibraryEntryRoutesDto.fromDomain))
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
            val statusChangeResult = for {
              u <- Uid.fromString(uid)
              s <- Status.fromString(status)
            } yield service.changeStatus(u, s)

            onComplete(Future.fromTry(statusChangeResult).flatten) {
              case Success(_) =>
                complete(StatusCodes.OK)

              case Failure(err) =>
                log.error(s"Can't update library entry, uid: $uid, error - $err")
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
      case _: IllegalArgumentException => complete(StatusCodes.BadRequest, Error(t.getMessage))
      case UidNotExists(msg) => complete(StatusCodes.Forbidden, Error(msg))
      case _ => complete(StatusCodes.InternalServerError, Error("Can't update"))
    }

}
