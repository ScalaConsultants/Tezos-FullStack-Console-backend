package io.scalac.tezos.translator.routes
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Route}
import io.scalac.tezos.translator.config.Configuration
import io.scalac.tezos.translator.model.Error
import io.scalac.tezos.translator.routes.directives.DTOValidationDirective._
import io.scalac.tezos.translator.routes.directives.ReCaptchaDirective._
import io.scalac.tezos.translator.routes.dto.LibraryEntryRoutesDto
import io.scalac.tezos.translator.service.LibraryService

import scala.util.{Failure, Success}

class LibraryRoutes(service: LibraryService,
                    log: LoggingAdapter,
                    config: Configuration)(implicit as: ActorSystem) extends HttpRoutes with JsonHelper {
  override def routes: Route =
    (path ("library") & pathEndOrSingleSlash) {
      (post & withReCaptchaVerify(log, config.reCaptcha)(as) & withLibraryDTOValidation) { libraryDto =>
        val operationPerformed = service.addNew(libraryDto.toDomain)
        onComplete(operationPerformed) {
          case Success(_)   => complete(StatusCodes.OK)
          case Failure(err) =>
            log.error(s"Can't save library dto $libraryDto, error - $err")
            complete(StatusCodes.InternalServerError, Error("Can't save payload"))
        }
      } ~
        (get & parameters('limit.as[Int].?)) { maybeLimit =>
          val limit = maybeLimit.getOrElse(config.dbUtility.defaultLimit)
          val operationPerformed = service.getAccepted(limit)(as.dispatcher)
          onComplete(operationPerformed) {
            case Success(libraryEntries) => complete(libraryEntries.map(LibraryEntryRoutesDto.fromDomain))
            case Failure(err)     =>
              log.error(s"Can't show accepted library models, limit - $limit error - $err")
              complete(StatusCodes.InternalServerError, Error("Can't get records"))
          }
      }
    }

  def withLibraryDTOValidation: Directive[Tuple1[LibraryEntryRoutesDto]] = withDTOValidation[LibraryEntryRoutesDto]

}
