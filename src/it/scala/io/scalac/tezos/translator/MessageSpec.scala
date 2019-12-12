package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.{ContactFormContent, EmailAddress, Errors, SendEmail}
import io.scalac.tezos.translator.repository.Emails2SendRepository
import io.scalac.tezos.translator.repository.dto.SendEmailDbDto
import io.scalac.tezos.translator.routes.dto.SendEmailRoutesDto
import io.scalac.tezos.translator.routes.{JsonHelper, MessageRoutes}
import io.scalac.tezos.translator.schema.Emails2SendTable
import io.scalac.tezos.translator.service.Emails2SendService
import org.scalatest.{Assertion, BeforeAndAfterEach, Matchers, WordSpec}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

//noinspection TypeAnnotation
class MessageSpec
  extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with BeforeAndAfterEach
  with JsonHelper {

    override def beforeEach(): Unit = DbTestBase.recreateTables()

    val testDb = DbTestBase.db

    val reCaptchaConfig = CaptchaConfig(checkOn = false, "", "", "")
    val emails2SendRepo = new Emails2SendRepository

    val email2SendService = new Emails2SendService(emails2SendRepo, testDb)
    val adminEmail = EmailAddress.fromString("tezos-console-admin@scalac.io").get
    val messageRoute: Route = new MessageRoutes(email2SendService, system.log, reCaptchaConfig, adminEmail).routes

    private def checkValidationErrorsWithExpected(dto: SendEmailRoutesDto, expectedErrors: List[String]): Assertion = {
      Post("/message", dto) ~> messageRoute ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[Errors].errors should contain theSameElementsAs expectedErrors
      }
    }

    private def getAllSendEmailsFromDb: Seq[SendEmailDbDto] = {
      val queryFuture = testDb.run(Emails2SendTable.emails2Send.result)
      Await.result(queryFuture, 1 second)
    }

    "message endpoint" should {
      "validate dto before storing" in {
        val invalidDto = SendEmailRoutesDto("", "", "", "")
        val expectedErrorsList1 = List("name field is empty", "phone field is empty", "email field is empty", "content field is empty")
        checkValidationErrorsWithExpected(invalidDto, expectedErrorsList1)

        val dtoInvalidPhoneAndEmail = SendEmailRoutesDto("name", "+7777777777777777777777", "email@@@gmail.com", "whatsup man")
        val expectedErrorsList2 = List("invalid phone number - +7777777777777777777777", "invalid email - email@@@gmail.com")
        checkValidationErrorsWithExpected(dtoInvalidPhoneAndEmail, expectedErrorsList2)

        val dtoNameAndEmailToLong = SendEmailRoutesDto(
          "123456789012345678901234567890123456789012345678901234567890123456789012345678" +
            "90123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678" +
            "901234567890123456789012345678901234567890123456789012345678901234567890",
          "+77777777777",
          "123456789012345678901234567890123456789012345678901234567890123456789012345678" +
            "90123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678" +
            "901234567890123456789012345678901234567890123456789012345678901234567890@gmail.com",
          "whatsup man")
        val expectedErrorsList3 = List("field name is too long, max length - 255", "field email is too long, max length - 255")
        checkValidationErrorsWithExpected(dtoNameAndEmailToLong, expectedErrorsList3)

        val tableActual = getAllSendEmailsFromDb
        tableActual.isEmpty shouldBe true
      }

      "save proper dto" in {
        val validDto = SendEmailRoutesDto("name", "+77072123434", "email@gmail.com", "I wanna pizza")
        Post("/message", validDto) ~> messageRoute ~> check {
          status shouldBe StatusCodes.OK
        }
        val tableActual = getAllSendEmailsFromDb

        tableActual.headOption.isEmpty shouldBe false
        val addedRecord = tableActual.head
        val maybeSendEmailModel = SendEmail.fromSendEmailDbDto(addedRecord)
        maybeSendEmailModel shouldBe a[Success[_]]

        val sendEmailModel = maybeSendEmailModel.get
        sendEmailModel.to shouldBe adminEmail

        sendEmailModel.subject shouldBe "Contact request"

        sendEmailModel.content shouldBe a[ContactFormContent]

        val content = sendEmailModel.content.asInstanceOf[ContactFormContent]

        content.name shouldBe "name"
        content.phone shouldBe "+77072123434"
        content.email shouldBe "email@gmail.com"
        content.content shouldBe "I wanna pizza"
      }
    }
}
