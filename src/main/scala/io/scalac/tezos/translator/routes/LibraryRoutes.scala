package io.scalac.tezos.translator.routes
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Route}
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.{Error, LibraryDTO}
import io.scalac.tezos.translator.service.LibraryService
import io.scalac.tezos.translator.routes.util.ReCaptchaDirective._
import util.DTOValidationDirective._

import scala.util.{Failure, Success}

class LibraryRoutes(service: LibraryService,
                    log: LoggingAdapter,
                    reCaptchaConfig: CaptchaConfig)(implicit as: ActorSystem)  extends HttpRoutes with JsonHelper {
  override def routes: Route =
    (path ("library") & pathEndOrSingleSlash & withReCaptchaVerify(log, reCaptchaConfig)(as)
      & withLibraryDTOValidation) { libraryDto =>
      val operationPerformed = service.addNew(libraryDto)
      onComplete(operationPerformed) {
        case Success(_)   => complete(StatusCodes.OK)
        case Failure(err) =>
          log.error(s"Can't save library dto $libraryDto, error - $err")
          complete(StatusCodes.InternalServerError, Error("Can't save payload"))
      }
    }

  def withLibraryDTOValidation: Directive[Tuple1[LibraryDTO]] = withDTOValidation[LibraryDTO]

}
