package io.scalac.tezos.translator.routes.dto

import cats.data.NonEmptyList
import io.scalac.tezos.translator.model.UserCredentials
import io.scalac.tezos.translator.routes.dto.DTOValidation.{DTOValidationError, FieldIsEmpty, FieldIsInvalid, FieldToLong}
import org.scalatest.{Matchers, WordSpec}

class DTOValidationSpec extends WordSpec with Matchers {

  val strign255chars: String =
    "verylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstring" +
      "verylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstring" +
      "verylongstringverylongstring123"


  "DTOValidation.validateSendEmailDTO" should {


    "pass validation" when {

      "all data is given" in {
        val input = validSendEmailRoutesDto()

        DTOValidation.validateSendEmailDTO(input) shouldBe Right(input)
      }

      "very long name is given" in {
        val input = validSendEmailRoutesDto(name = strign255chars)

        DTOValidation.validateSendEmailDTO(input) shouldBe Right(input)
      }

      "no phone is given" in {
        val input = validSendEmailRoutesDto(phone = None)

        DTOValidation.validateSendEmailDTO(input) shouldBe Right(input)
      }

      "phone is empty" in {
        val input = validSendEmailRoutesDto(phone = Some(""))

        DTOValidation.validateSendEmailDTO(input) shouldBe Right(input.copy(phone = None))
      }

      "no email is given" in {
        val input = validSendEmailRoutesDto(email = None)

        DTOValidation.validateSendEmailDTO(input) shouldBe Right(input)
      }

      "email is empty" in {
        val input = validSendEmailRoutesDto(email = Some(""))

        DTOValidation.validateSendEmailDTO(input) shouldBe Right(input.copy(email = None))
      }

    }

    "not pass validation" when {

      "all fields are empty" in {
        val input = SendEmailRoutesDto("", None, None, "")

        DTOValidation.validateSendEmailDTO(input) shouldBe
          validationError(FieldIsInvalid("email, phone", "At least one field should be filled"), FieldIsEmpty("name"), FieldIsEmpty("content"))
      }

      "name is empty" in {
        val input = validSendEmailRoutesDto(name = "")

        DTOValidation.validateSendEmailDTO(input) shouldBe validationError(FieldIsEmpty("name"))
      }

      "name is too long" in {
        val input = validSendEmailRoutesDto(name = strign255chars + "1")

        DTOValidation.validateSendEmailDTO(input) shouldBe validationError(FieldToLong("name", 255))
      }

      "content is empty" in {
        val input = validSendEmailRoutesDto(content = "")

        DTOValidation.validateSendEmailDTO(input) shouldBe validationError(FieldIsEmpty("content"))
      }

      "phone is invalid" in {
        val input = validSendEmailRoutesDto(phone = Some("abcd"))

        DTOValidation.validateSendEmailDTO(input) shouldBe validationError(FieldIsInvalid("phone", "abcd"))
      }

      "email is invalid" in {
        val input = validSendEmailRoutesDto(email = Some("emailemail.com"))

        DTOValidation.validateSendEmailDTO(input) shouldBe validationError(FieldIsInvalid("email", "emailemail.com"))
      }

      "phone and email is not given" in {
        val input = validSendEmailRoutesDto(phone = None, email = None)

        DTOValidation.validateSendEmailDTO(input) shouldBe validationError(FieldIsInvalid("email, phone", "At least one field should be filled"))
      }

      "phone and email are empty" in {
        val input = validSendEmailRoutesDto(phone = Some(""), email = Some(""))

        DTOValidation.validateSendEmailDTO(input) shouldBe validationError(FieldIsInvalid("email, phone", "At least one field should be filled"))
      }
    }

  }

  "DTOValidation.validateLibraryEntryRoutesDto" should {

    "pass validation" when {

      "all data is given" in {
        val input = validLibraryEntryRoutesDto()

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input)
      }

      "author is not given" in {
        val input = validLibraryEntryRoutesDto(author = None)

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input)
      }

      "author is an empty string" in {
        val input = validLibraryEntryRoutesDto(author = Some(""))

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input.copy(author = None))
      }

      "email is not given" in {
        val input = validLibraryEntryRoutesDto(email = None)

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input)
      }

      "email is an empty string" in {
        val input = validLibraryEntryRoutesDto(email = Some(""))

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input.copy(email = None))
      }

      "description is not given" in {
        val input = validLibraryEntryRoutesDto(description = None)

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input)
      }

      "description is an empty string" in {
        val input = validLibraryEntryRoutesDto(description = None)

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input.copy(description = None))
      }

      "author, email is not given" in {
        val input = validLibraryEntryRoutesDto(author = None, email = None)

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input)
      }

      "email, description is not given" in {
        val input = validLibraryEntryRoutesDto(email = None, description = None)

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input)
      }

      "author, description is not given" in {
        val input = validLibraryEntryRoutesDto(author = None, description = None)

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input)
      }

      "author, email, description is not given" in {
        val input = validLibraryEntryRoutesDto(author = None, email = None, description = None)

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe Right(input)
      }

    }

    "not pass validation" when {
      "all fields are empty" in {
        val input = LibraryEntryRoutesDto(
          title = "",
          author = None,
          email = None,
          description = None,
          micheline = "",
          michelson = ""
        )

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe
          validationError(FieldIsEmpty("title"), FieldIsEmpty("micheline"), FieldIsEmpty("michelson"))
      }

      "title is empty" in {
        val input = validLibraryEntryRoutesDto(title = "")

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe validationError(FieldIsEmpty("title"))
      }

      "email is invalid" in {
        val input = validLibraryEntryRoutesDto(email = Some("emailemail.com"))

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe validationError(FieldIsInvalid("email", "emailemail.com"))
      }

      "micheline is empty" in {
        val input = validLibraryEntryRoutesDto(micheline = "")

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe validationError(FieldIsEmpty("micheline"))
      }

      "michelson is empty" in {
        val input = validLibraryEntryRoutesDto(michelson = "")

        DTOValidation.validateLibraryEntryRoutesDto(input) shouldBe validationError(FieldIsEmpty("michelson"))
      }
    }
  }

  "DTOValidation.validateUserCredentials" should {

    "pass validation" when {

      "all data is given" in {
        val input = validUserCredentials()

        DTOValidation.validateUserCredentials(input) shouldBe Right(input)
      }

    }

    "not pass validation" when {
      "all fields are empty" in {
        val input = UserCredentials(username = "", password = "")

        DTOValidation.validateUserCredentials(input) shouldBe validationError(FieldIsEmpty("username"), FieldIsEmpty("password"))
      }

      "username is empty" in {
        val input = validUserCredentials(username = "")

        DTOValidation.validateUserCredentials(input) shouldBe validationError(FieldIsEmpty("username"))
      }

      "password is empty" in {
        val input = validUserCredentials(password = "")

        DTOValidation.validateUserCredentials(input) shouldBe validationError(FieldIsEmpty("password"))
      }
    }
  }

  private def validationError(errors: DTOValidationError*) = Left(NonEmptyList.fromListUnsafe(errors.toList))

  private def validSendEmailRoutesDto(
    name: String = "name",
    phone: Option[String] = Some("123123123"),
    email: Option[String] = Some("email@email.com"),
    content: String = "content"
  ): SendEmailRoutesDto = SendEmailRoutesDto(name, phone, email, content)

  private def validLibraryEntryRoutesDto(
    title: String = "title",
    author: Option[String] = Some("author"),
    email: Option[String] = Some("email@email.com"),
    description: Option[String] = Some("description"),
    micheline: String = "micheline",
    michelson: String = "michelson"
  ): LibraryEntryRoutesDto = LibraryEntryRoutesDto(title, author, email, description, micheline, michelson)

  private def validUserCredentials(
    username: String = "user",
    password: String = "pass"
  ): UserCredentials = UserCredentials(username, password)
}

