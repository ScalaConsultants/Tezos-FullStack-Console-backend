package io.scalac.tezos.translator.routes.util

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Directives}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.scalac.tezos.translator.model.DTO.ErrorsDTO
import io.scalac.tezos.translator.routes.JsonHelper
import io.scalac.tezos.translator.routes.util.DTOValidation._

object DTOValidationDirective extends Directives with JsonHelper {

  def withDTOValidation[T : FromEntityUnmarshaller : DTOValidation]: Directive[Tuple1[T]] =
    entity(as[T]).flatMap { ent =>
      DTOValidation(ent) match {
        case Right(value) => provide(value)
        case Left(errors) =>
          val errorsList = errors.map(convertValidationErrorsToString).toList
          complete(StatusCodes.BadRequest, ErrorsDTO(errorsList))
      }
    }

  def convertValidationErrorsToString: PartialFunction[DTOValidationError, String] = {
    case FieldToLong(field, maxLength) => s"Field $field is to long, max length - $maxLength"
    case NameIsEmpty                   => "Name field is empty"
    case PhoneIsEmpty                  => "Phone field is empty"
    case PhoneIsInvalid(phone)         => s"Invalid phone number $phone"
    case EmailIsEmpty                  => "Email field is empty"
    case EmailIsInvalid(email)         => s"Invalid email $email"
    case ContentIsEmpty                => "Content fields is empty"
  }

}
