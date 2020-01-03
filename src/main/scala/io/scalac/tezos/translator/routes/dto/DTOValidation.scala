package io.scalac.tezos.translator.routes.dto

import cats.data.NonEmptyList
import cats.instances.parallel._
import cats.syntax.either._
import cats.syntax.parallel._
import io.scalac.tezos.translator.model._
import io.scalac.tezos.translator.routes.dto.DTOValidation.ValidationResult

trait DTOValidation[T] {

  def validate(value: T): ValidationResult[T]

}

object DTOValidation {

  val maxTinyLength     = 255
  val maxUsernameLength = 30

  type ValidationResult[A] = Either[NonEmptyList[DTOValidationError], A]

  def apply[T](value: T)(implicit validator: DTOValidation[T]): ValidationResult[T] =
    validator.validate(value)

  sealed trait DTOValidationError

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
    val checkingNameResult: ValidationResult[String] =
      checkStringNotEmptyAndLength(dto.name, maxTinyLength, FieldIsEmpty("name"), FieldToLong("name", maxTinyLength))

    val checkingPhoneIsValid: Either[NonEmptyList[DTOValidationError], Option[String]] =
      dto.phone
        .map(maybePhone => checkStringMatchRegExp(maybePhone, phoneRegex, FieldIsInvalid("phone number", maybePhone)).map(Some(_)))
        .getOrElse(None.asRight)

    val checkContentNotEmpty: ValidationResult[String] = checkStringNotEmpty(dto.content, FieldIsEmpty("content"))
    val checkEmail: Either[NonEmptyList[DTOValidationError], Option[String]] =
      dto.email.map(mail => checkEmailIsValid(mail).map(x => Some(x.toLowerCase))).getOrElse(None.asRight)

    if (dto.email.isEmpty && dto.phone.isEmpty) {
      (checkingNameResult, NonEmptyList.one(FieldIsEmpty("Both, Email field is empty and Phone")).asLeft, checkEmail, checkContentNotEmpty)
        .parMapN(SendEmailRoutesDto.apply)
    } else {
      (checkingNameResult, checkingPhoneIsValid, checkEmail, checkContentNotEmpty)
        .parMapN(SendEmailRoutesDto.apply)
    }
  }

  private def checkEmailIsValid(email: String): ValidationResult[String] =
    checkStringNotEmptyAndLength(email, maxTinyLength, FieldIsEmpty("email"), FieldToLong("email", maxTinyLength))
      .flatMap { mail =>
        EmailAddress
          .fromString(mail)
          .toEither
          .bimap(_ => NonEmptyList.one(FieldIsInvalid("email", mail)), a => a.toString)
      }
  private def checkAuthorIsValid(value: String, name: String = "author"): ValidationResult[String] =
    checkStringNotEmptyAndLength(value, maxTinyLength, FieldIsEmpty(name), FieldToLong(name, maxTinyLength))
  private def checkDescriptionsValid(value: String, name: String = "description"): ValidationResult[String] =
    checkStringNotEmptyAndLength(value, maxTinyLength, FieldIsEmpty(name), FieldToLong(name, maxTinyLength))

  implicit val LibraryDTOValidation: DTOValidation[LibraryEntryRoutesDto] = { dto =>
    val checkName =
      checkStringNotEmptyAndLength(dto.title, maxTinyLength, FieldIsEmpty("name"), FieldToLong("name", maxTinyLength))
    val checkAuthor =
      dto.author.map(author => checkAuthorIsValid(author).map(Some(_))).getOrElse(None.asRight)
    val checkEmail =
      dto.email.map(mail => checkEmailIsValid(mail).map(x => Some(x.toLowerCase))).getOrElse(None.asRight)
    val checkDescription =
      dto.description.map(description => checkDescriptionsValid(description).map(Some(_))).getOrElse(None.asRight)
    val checkMicheline =
      checkStringNotEmpty(dto.micheline, FieldIsEmpty("micheline"))
    val checkMichelson =
      checkStringNotEmpty(dto.michelson, FieldIsEmpty("michelson"))

    (checkName, checkAuthor, checkEmail, checkDescription, checkMicheline, checkMichelson).parMapN(LibraryEntryRoutesDto.apply)
  }

  implicit val UserCredentialsValidation: DTOValidation[UserCredentials] = { dto =>
    val checkUsername =
      checkStringNotEmptyAndLength(dto.username, maxUsernameLength, FieldIsEmpty("username"), FieldToLong("username", maxUsernameLength))
    val checkPassword = checkStringNotEmpty(dto.password, FieldIsEmpty("password"))
    (checkUsername, checkPassword).parMapN(UserCredentials.apply)
  }

  val phoneRegex: String =
    """^\+?\d{6,18}$"""

}
