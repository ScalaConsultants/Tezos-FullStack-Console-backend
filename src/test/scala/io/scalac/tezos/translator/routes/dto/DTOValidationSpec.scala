package io.scalac.tezos.translator.routes.dto

import cats.data.NonEmptyList
import eu.timepit.refined.collection.NonEmpty
import io.scalac.tezos.translator.model.types.ContactData.{Content, Name, NameAndEmailReq, Phone, PhoneReq, RefinedEmailString}
import io.scalac.tezos.translator.routes.dto.DTOValidation.{DTOValidationError, FieldIsEmpty, FieldIsInvalid}
import org.scalatest.{Matchers, WordSpec}
import eu.timepit.refined.refineMV

class DTOValidationSpec extends WordSpec with Matchers {

  "DTOValidation.validateSendEmailDTO" should {


    "pass validation" when {

      "all data is given" in {
        val input = validSendEmailRoutesDto()

        DTOValidation.validateSendEmailDTO(input) shouldBe Right(input)
      }

      "no phone is given" in {
        val input = validSendEmailRoutesDto(phone = None)

        DTOValidation.validateSendEmailDTO(input) shouldBe Right(input)
      }

      "no email is given" in {
        val input = validSendEmailRoutesDto(email = None)

        DTOValidation.validateSendEmailDTO(input) shouldBe Right(input)
      }

    }

    "not pass validation" when {

      "email is invalid" in {
        val input = validSendEmailRoutesDto(email = Some(RefinedEmailString(refineMV[NameAndEmailReq]("emailemail.com"))))

        DTOValidation.validateSendEmailDTO(input) shouldBe validationError(FieldIsInvalid("email", "emailemail.com"))
      }

      "phone and email is not given" in {
        val input = validSendEmailRoutesDto(phone = None, email = None)

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
        val input = validLibraryEntryRoutesDto(email = Some(RefinedEmailString(refineMV[NameAndEmailReq]("emailemail.com"))))

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

  private def validationError(errors: DTOValidationError*) = Left(NonEmptyList.fromListUnsafe(errors.toList))

  private def validSendEmailRoutesDto(
                                       name: Name = Name(refineMV[NameAndEmailReq]("name")),
                                       phone: Option[Phone] = Some(Phone(refineMV[PhoneReq]("123123123"))),
                                       email: Option[RefinedEmailString] = Some(RefinedEmailString(refineMV[NameAndEmailReq]("email@email.com"))),
                                       content: Content = Content(refineMV[NonEmpty]("content"))
  ): SendEmailRoutesDto = SendEmailRoutesDto(name, phone, email, content)

  private def validLibraryEntryRoutesDto(
    title: String = "title",
    author: Option[String] = Some("author"),
    email: Option[RefinedEmailString] = Some(RefinedEmailString(refineMV[NameAndEmailReq]("email@email.com"))),
    description: Option[String] = Some("description"),
    micheline: String = "micheline",
    michelson: String = "michelson"
  ): LibraryEntryRoutesDto = LibraryEntryRoutesDto(title, author, email, description, micheline, michelson)

}

