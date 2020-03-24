package io.scalac.tezos.translator.routes.dto

import cats.data.NonEmptyList
import cats.instances.either._
import cats.instances.option._
import cats.instances.parallel._
import cats.syntax.either._
import cats.syntax.parallel._
import cats.syntax.traverse._
import eu.timepit.refined.refineV
import io.scalac.tezos.translator.model._
import io.scalac.tezos.translator.model.types.ContactData.{ EmailReq, EmailS }
import io.scalac.tezos.translator.routes.dto.DTO.{ ErrorDTO, Errors }
import io.scalac.tezos.translator.routes.dto.DTOValidation.ValidationResult
import sttp.model.StatusCode

import scala.concurrent.{ ExecutionContext, Future }

trait DTOValidation[T] {

  def validate(value: T): ValidationResult[T]

}

object DTOValidation {
  type ValidationResult[A] = Either[NonEmptyList[DTOValidationError], A]
  val maxTinyLength     = 255
  val maxUsernameLength = 30

  def validateDto[T: DTOValidation](value: T)(implicit ec: ExecutionContext): Future[Either[(ErrorDTO, StatusCode), T]] =
    Future {
      DTOValidation(value) match {
        case Right(value) => value.asRight
        case Left(errors) =>
          val errorsList = errors.map(convertValidationErrorsToString).toList
          (Errors(errorsList), StatusCode.BadRequest).asLeft
      }
    }

  def apply[T](value: T)(implicit validator: DTOValidation[T]): ValidationResult[T] =
    validator.validate(value)

  def convertValidationErrorsToString: PartialFunction[DTOValidationError, String] = {
    case FieldToLong(field, maxLength)    => s"field $field is too long, max length - $maxLength"
    case FieldIsEmpty(fieldName)          => s"$fieldName field is empty"
    case FieldIsInvalid(fieldName, field) => s"invalid $fieldName - $field"
  }

  def checkStringNotEmptyAndLength(
     string: String,
     maxLength: Int,
     onEmpty: => DTOValidationError,
     whenMaxLengthExceeds: => DTOValidationError
   ): ValidationResult[String] =
    checkStringNotEmpty(string, onEmpty)
      .flatMap(checkStringLength(_, maxLength, whenMaxLengthExceeds))

  def checkStringNotEmpty(string: String, onEmpty: => DTOValidationError): ValidationResult[String] =
    if (string.trim.isEmpty)
      NonEmptyList.one(onEmpty).asLeft
    else
      string.asRight

  def checkStringLength(string: String, maxLength: Int, whenMaxLengthExceeds: => DTOValidationError): ValidationResult[String] =
    if (string.length > maxLength)
      NonEmptyList.one(whenMaxLengthExceeds).asLeft
    else
      string.asRight

  def checkStringMatchRegExp(string: String, regExp: String, onNonMatch: => DTOValidationError): ValidationResult[String] =
    if (string.matches(regExp))
      string.asRight
    else
      NonEmptyList.one(onNonMatch).asLeft

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

  def validateLibraryEntryRoutesNewDto: LibraryEntryRoutesNewDto => ValidationResult[LibraryEntryRoutesNewDto] = { dto =>
    val checkEmail: Either[NonEmptyList[DTOValidationError], Option[EmailS]] =
      dto.email.traverse(checkEmailIsValid)

    checkEmail.map(maybeEmail => dto.copy(email = maybeEmail))
  }

  sealed trait DTOValidationError extends Product with Serializable

  implicit val SendEmailDTOValidation: DTOValidation[SendEmailRoutesDto] = { dto => validateSendEmailDTO(dto) }

  final case class FieldToLong(field: String, maxLength: Int) extends DTOValidationError

  final case class FieldIsEmpty(field: String) extends DTOValidationError

  implicit val LibraryDTOValidation: DTOValidation[LibraryEntryRoutesNewDto] = { dto => validateLibraryEntryRoutesNewDto(dto) }

  final case class FieldIsInvalid(fieldName: String, field: String) extends DTOValidationError

}
