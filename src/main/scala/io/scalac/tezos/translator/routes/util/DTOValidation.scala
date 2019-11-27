package io.scalac.tezos.translator.routes.util

import cats.data.NonEmptyList
import cats.syntax.either._
import cats.syntax.parallel._
import cats.instances.parallel._
import io.scalac.tezos.translator.model._
import io.scalac.tezos.translator.routes.util.DTOValidation.ValidationResult

trait DTOValidation[T] {

  def validate(value: T): ValidationResult[T]

}

object DTOValidation {

  val maxTinyLength = 255

  type ValidationResult[A] = Either[NonEmptyList[DTOValidationError], A]

  def apply[T](value: T)(implicit validator: DTOValidation[T]): ValidationResult[T] =
    validator.validate(value)

  sealed trait DTOValidationError

  final case class FieldToLong(field: String, maxLength: Int) extends DTOValidationError

  final case class FieldIsEmpty(field: String) extends DTOValidationError

  final case class FieldIsInvalid(fieldName: String, field: String) extends DTOValidationError

  def checkStringNotEmpty(string: String,
                          onEmpty: => DTOValidationError): ValidationResult[String] = {
    if (string.trim.isEmpty)
      NonEmptyList.one(onEmpty).asLeft
    else
      string.asRight
  }

  def checkStringNotEmptyAndLength(string: String,
                                   maxLength: Int,
                                   onEmpty: => DTOValidationError,
                                   whenMaxLengthExceeds: => DTOValidationError): ValidationResult[String] = {
    checkStringNotEmpty(string, onEmpty)
      .flatMap(checkStringLength(_, maxLength, whenMaxLengthExceeds))
  }

  def checkStringMatchRegExp(string: String,
                             regExp: String,
                             onNonMatch: => DTOValidationError): ValidationResult[String] = {
    if (string.matches(regExp))
      string.asRight
    else
      NonEmptyList.one(onNonMatch).asLeft
  }

  def checkStringLength(string: String,
                        maxLength: Int,
                        whenMaxLengthExceeds: => DTOValidationError): ValidationResult[String] = {
    if (string.length > maxLength)
      NonEmptyList.one(whenMaxLengthExceeds).asLeft
    else
      string.asRight
  }

  implicit val SendEmailDTOValidation: DTOValidation[SendEmailJsonDTO] = { dto =>
    val checkingNameResult: ValidationResult[String] =
      checkStringNotEmptyAndLength(dto.name, maxTinyLength, FieldIsEmpty("name"), FieldToLong("name", maxTinyLength))

    val checkingPhoneIsValid: ValidationResult[String] =
      checkStringNotEmpty(dto.phone, FieldIsEmpty("phone"))
        .flatMap(
          maybePhone => checkStringMatchRegExp(maybePhone, phoneRegex, FieldIsInvalid("phone number", maybePhone))
        )

    val checkContentNotEmpty: ValidationResult[String] = checkStringNotEmpty(dto.content, FieldIsEmpty("content"))

    (checkingNameResult, checkingPhoneIsValid, checkEmailIsValid(dto.email), checkContentNotEmpty)
      .parMapN(SendEmailJsonDTO)
  }

  private def checkEmailIsValid(email: String): ValidationResult[String] =
    checkStringNotEmptyAndLength(email, maxTinyLength, FieldIsEmpty("email"), FieldToLong("email", maxTinyLength))
      .flatMap(
        mail => checkStringMatchRegExp(mail, emailRegex, FieldIsInvalid("email", mail))
      )

  implicit val LibraryDTOValidation: DTOValidation[LibraryJsonDTO] = { dto =>
    val checkName =
      checkStringNotEmptyAndLength(dto.name, maxTinyLength, FieldIsEmpty("name"), FieldToLong("name", maxTinyLength))
    val checkAuthor =
      checkStringNotEmptyAndLength(dto.author, maxTinyLength, FieldIsEmpty("author"), FieldToLong("author", maxTinyLength))
    val checkEmail =
      dto.email.map(mail => checkEmailIsValid(mail).map(Some(_))).getOrElse(None.asRight)
    val checkDescription =
      checkStringNotEmpty(dto.description, FieldIsEmpty("description"))
    val checkMicheline =
      checkStringNotEmpty(dto.micheline, FieldIsEmpty("micheline"))
    val checkMichelson =
      checkStringNotEmpty(dto.michelson, FieldIsEmpty("michelson"))

    (checkName, checkAuthor, checkEmail, checkDescription, checkMicheline, checkMichelson).parMapN(LibraryJsonDTO)
  }

  val emailRegex: String =
    """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])"""

  val phoneRegex: String =
    """^\+?\d{6,18}$"""

}
