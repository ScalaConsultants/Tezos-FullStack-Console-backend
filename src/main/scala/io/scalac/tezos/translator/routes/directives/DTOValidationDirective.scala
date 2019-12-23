package io.scalac.tezos.translator.routes.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Directives}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.scalac.tezos.translator.model.Errors
import io.scalac.tezos.translator.routes.dto.DTOValidation
import io.scalac.tezos.translator.routes.dto.DTOValidation._

object DTOValidationDirective extends Directives {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  def withDTOValidation[T: FromEntityUnmarshaller: DTOValidation]: Directive[Tuple1[T]] =
    entity(as[T]).flatMap { ent =>
      DTOValidation(ent) match {
        case Right(value) => provide(value)
        case Left(errors) =>
          val errorsList = errors.map(convertValidationErrorsToString).toList
          complete(StatusCodes.BadRequest, Errors(errorsList))
      }
    }

  def convertValidationErrorsToString: PartialFunction[DTOValidationError, String] = {
    case FieldToLong(field, maxLength)    => s"field $field is too long, max length - $maxLength"
    case FieldIsEmpty(fieldName)          => s"$fieldName field is empty"
    case FieldIsInvalid(fieldName, field) => s"invalid $fieldName - $field"
  }

}
