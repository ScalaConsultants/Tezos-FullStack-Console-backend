package io.scalac.tezos.translator.routes.util

import cats.data.NonEmptyList
import cats.syntax.either._
import cats.syntax.parallel._
import cats.instances.parallel._
import io.scalac.tezos.translator.model.SendEmailDTO
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

  case object NameIsEmpty extends DTOValidationError

  case object PhoneIsEmpty extends DTOValidationError

  final case class PhoneIsInvalid(phoneString: String) extends DTOValidationError

  case object EmailIsEmpty extends DTOValidationError

  final case class EmailIsInvalid(emailString: String) extends DTOValidationError

  case object ContentIsEmpty extends DTOValidationError

  def checkStringNotEmpty(string: String,
                          onEmpty: DTOValidationError): ValidationResult[String] = {
    if (string.trim.isEmpty)
      NonEmptyList.one(onEmpty).asLeft
    else
      string.asRight
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

  implicit val SendEmailDTOValidation: DTOValidation[SendEmailDTO] = { dto =>
    val checkingNameResult: ValidationResult[String] =
      checkStringNotEmpty(dto.name, NameIsEmpty)
      .flatMap(
        name => checkStringLength(name, maxTinyLength, FieldToLong("name", maxTinyLength))
      )
    val checkingPhoneNotEmpty: ValidationResult[String] = checkStringNotEmpty(dto.phone, PhoneIsEmpty)

    val checkingPhoneIsValid: ValidationResult[String] = checkingPhoneNotEmpty
      .flatMap(
        maybePhone => checkStringMatchRegExp(maybePhone, """^\+?\d{6,18}$""", PhoneIsInvalid(maybePhone))
      )

    val checkingEmailNotEmpty: ValidationResult[String] = checkStringNotEmpty(dto.email, EmailIsEmpty)

    val checkingEmailIsValid: ValidationResult[String] = checkingEmailNotEmpty
      .flatMap(
        maybeEmail => checkStringMatchRegExp(maybeEmail, emailRegex, EmailIsInvalid(maybeEmail))
      )
      .flatMap(
        email => checkStringLength(email, maxTinyLength, FieldToLong("email", maxTinyLength))
      )

    val checkContentNotEmpty: ValidationResult[String] = checkStringNotEmpty(dto.content, ContentIsEmpty)

    (checkingNameResult, checkingPhoneIsValid, checkingEmailIsValid, checkContentNotEmpty)
      .parMapN(SendEmailDTO)
  }

  val emailRegex: String =
    """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])"""

}
