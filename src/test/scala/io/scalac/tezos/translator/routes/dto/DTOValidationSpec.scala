package io.scalac.tezos.translator.routes.dto

import cats.data.NonEmptyList
import cats.syntax.option._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineMV
import io.scalac.tezos.translator.model.types.ContactData.{ Content, EmailReq, EmailS, Name, NameReq, Phone, PhoneReq }
import io.scalac.tezos.translator.model.types.Library._
import io.scalac.tezos.translator.routes.dto.DTOValidation.{ DTOValidationError, FieldIsInvalid }
import org.scalatest.{ Matchers, WordSpec }

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

      "phone and email is not given" in {
        val input = validSendEmailRoutesDto(phone = None, email = None)

        DTOValidation.validateSendEmailDTO(input) shouldBe validationError(
           FieldIsInvalid("email, phone", "At least one field should be filled")
        )
      }

    }

  }

  "DTOValidation.validateLibraryEntryRoutesNewDto" should {

    "pass validation" when {

      "all data is given" in {
        val input = validLibraryEntryRoutesNewDto()

        DTOValidation.validateLibraryEntryRoutesNewDto(input) shouldBe Right(input)
      }

      "author is not given" in {
        val input = validLibraryEntryRoutesNewDto(author = None)

        DTOValidation.validateLibraryEntryRoutesNewDto(input) shouldBe Right(input)
      }

      "email is not given" in {
        val input = validLibraryEntryRoutesNewDto(email = None)

        DTOValidation.validateLibraryEntryRoutesNewDto(input) shouldBe Right(input)
      }

      "description is not given" in {
        val input = validLibraryEntryRoutesNewDto(description = None)

        DTOValidation.validateLibraryEntryRoutesNewDto(input) shouldBe Right(input)
      }

      "description is an empty string" in {
        val input = validLibraryEntryRoutesNewDto(description = None)

        DTOValidation.validateLibraryEntryRoutesNewDto(input) shouldBe Right(input.copy(description = None))
      }

      "author, email is not given" in {
        val input = validLibraryEntryRoutesNewDto(author = None, email = None)

        DTOValidation.validateLibraryEntryRoutesNewDto(input) shouldBe Right(input)
      }

      "email, description is not given" in {
        val input = validLibraryEntryRoutesNewDto(email = None, description = None)

        DTOValidation.validateLibraryEntryRoutesNewDto(input) shouldBe Right(input)
      }

      "author, description is not given" in {
        val input = validLibraryEntryRoutesNewDto(author = None, description = None)

        DTOValidation.validateLibraryEntryRoutesNewDto(input) shouldBe Right(input)
      }

      "author, email, description is not given" in {
        val input = validLibraryEntryRoutesNewDto(author = None, email = None, description = None)

        DTOValidation.validateLibraryEntryRoutesNewDto(input) shouldBe Right(input)
      }

    }

  }

  private def validationError(errors: DTOValidationError*) = Left(NonEmptyList.fromListUnsafe(errors.toList))

  private def validSendEmailRoutesDto(
     name: Name            = Name(refineMV[NameReq]("name")),
     phone: Option[Phone]  = Some(Phone(refineMV[PhoneReq]("123123123"))),
     email: Option[EmailS] = Some(EmailS(refineMV[EmailReq]("email@email.com"))),
     content: Content      = Content(refineMV[NonEmpty]("content"))
   ): SendEmailRoutesDto = SendEmailRoutesDto(name, phone, email, content)

  private def validLibraryEntryRoutesNewDto(
     title: Title                     = Title(refineMV[NotEmptyAndNotLong]("title")),
     author: Option[Author]           = Author(refineMV[NotEmptyAndNotLong]("author")).some,
     email: Option[EmailS]            = Some(EmailS(refineMV[EmailReq]("email@email.com"))),
     description: Option[Description] = Description(refineMV[NotEmptyAndNotLong]("description")).some,
     micheline: Micheline             = Micheline(refineMV[NonEmpty]("micheline")),
     michelson: Michelson             = Michelson(refineMV[NonEmpty]("michelson"))
   ): LibraryEntryRoutesNewDto = LibraryEntryRoutesNewDto(title, author, email, description, micheline, michelson)

}
