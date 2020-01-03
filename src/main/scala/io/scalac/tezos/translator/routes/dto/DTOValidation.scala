package io.scalac.tezos.translator.routes.dto

import cats.data.NonEmptyList
import cats.instances.parallel._
import cats.instances.option._
import cats.instances.either._
import cats.syntax.traverse._
import cats.syntax.either._
import cats.syntax.parallel._
import eu.timepit.refined.refineV
import io.scalac.tezos.translator.model._
import io.scalac.tezos.translator.model.types.ContactData.{EmailReq, EmailS}
import io.scalac.tezos.translator.routes.dto.DTO.{ErrorDTO, Errors}
import io.scalac.tezos.translator.routes.dto.DTOValidation.ValidationResult
import sttp.model.StatusCode
import scala.concurrent.{ExecutionContext, Future}

trait DTOValidation[T] {

  def validate(value: T): ValidationResult[T]

}

object DTOValidation {

  val maxTinyLength     = 255
  val maxUsernameLength = 30

  type ValidationResult[A] = Either[NonEmptyList[DTOValidationError], A]

  def apply[T](value: T)(implicit validator: DTOValidation[T]): ValidationResult[T] =
    validator.validate(value)

  def validateDto[T: DTOValidation](value: T)(implicit ec: ExecutionContext): Future[Either[(ErrorDTO, StatusCode), T]] =
    Future {
      DTOValidation(value) match {
        case Right(value) => value.asRight
        case Left(errors) =>
          val errorsList = errors.map(convertValidationErrorsToString).toList
          (Errors(errorsList), StatusCode.BadRequest).asLeft
      }
    }

  def convertValidationErrorsToString: PartialFunction[DTOValidationError, String] = {
    case FieldToLong(field, maxLength)    => s"field $field is too long, max length - $maxLength"
    case FieldIsEmpty(fieldName)          => s"$fieldName field is empty"
    case FieldIsInvalid(fieldName, field) => s"invalid $fieldName - $field"
  }

  sealed trait DTOValidationError extends Product with Serializable

  final case class FieldToLong(field: String, maxLength: Int) extends DTOValidationError

  final case class FieldIsEmpty(field: String) extends DTOValidationError

  final case class FieldIsInvalid(fieldName: String, field: String) extends DTOValidationError

  def checkStringNotEmpty(string: String, onEmpty: => DTOValidationError): ValidationResult[String] =
    if (string.trim.isEmpty)
      NonEmptyList.one(onEmpty).asLeft
    else
      string.asRight

  def checkStringNotEmptyAndLength(
     string: String,
     maxLength: Int,
     onEmpty: => DTOValidationError,
     whenMaxLengthExceeds: => DTOValidationError
   ): ValidationResult[String] =
    checkStringNotEmpty(string, onEmpty)
      .flatMap(checkStringLength(_, maxLength, whenMaxLengthExceeds))

  def checkStringMatchRegExp(string: String, regExp: String, onNonMatch: => DTOValidationError): ValidationResult[String] =
    if (string.matches(regExp))
      string.asRight
    else
      NonEmptyList.one(onNonMatch).asLeft

  def checkStringLength(string: String, maxLength: Int, whenMaxLengthExceeds: => DTOValidationError): ValidationResult[String] =
    if (string.length > maxLength)
      NonEmptyList.one(whenMaxLengthExceeds).asLeft
    else
      string.asRight

  implicit val SendEmailDTOValidation: DTOValidation[SendEmailRoutesDto] = { dto =>
    validateSendEmailDTO(dto)
  }

  def validateSendEmailDTO: SendEmailRoutesDto => ValidationResult[SendEmailRoutesDto] = { dto =>
    val checkEmail: ValidationResult[Option[EmailS]] = dto.email.traverse(checkEmailIsValid)

    val phoneEmailNonEmptyCheck =
      if (dto.phone.isEmpty && checkEmail.right.exists(_.isEmpty)) {
        NonEmptyList.one(FieldIsInvalid("email, phone", "At least one field should be filled")).asLeft
      } else {
        ().asRight
      }

    val v = checkEmail.map(maybeEmail => dto.copy(email = maybeEmail))

    (phoneEmailNonEmptyCheck, v).parMapN((_, dto) => dto)

  }

  private def checkEmailIsValid(email: EmailS): ValidationResult[EmailS] =
    EmailAddress
      .fromString(email.v.value)
      .toEither
      .leftMap(_ => NonEmptyList.one(FieldIsInvalid("email", email.v.value)))
      .flatMap(
         a =>
           refineV[EmailReq](a.toString.toLowerCase) match {
             case Left(_)      => NonEmptyList.one(FieldIsInvalid("email", email.v.value)).asLeft
             case Right(value) => EmailS(value).asRight
           }
      )

  implicit val LibraryDTOValidation: DTOValidation[LibraryEntryRoutesDto] = { dto =>
    validateLibraryEntryRoutesDto(dto)
  }

  def validateLibraryEntryRoutesDto: LibraryEntryRoutesDto => ValidationResult[LibraryEntryRoutesDto] = { dto =>
    val checkEmail: Either[NonEmptyList[DTOValidationError], Option[EmailS]] =
      dto.email.traverse(checkEmailIsValid)

    checkEmail.map(maybeEmail => dto.copy(email = maybeEmail))
  }

}
