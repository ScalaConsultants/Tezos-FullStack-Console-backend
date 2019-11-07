package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.{Errors, SendEmailDTO, SendEmailModel}
import io.scalac.tezos.translator.repository.Emails2SendRepository
import io.scalac.tezos.translator.routes.{JsonHelper, MessageRoutes}
import io.scalac.tezos.translator.schema.Emails2SendTable
import io.scalac.tezos.translator.service.Emails2SendService
import org.scalatest.{Assertion, BeforeAndAfterAll, Matchers, WordSpec}
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class MessageSpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll with JsonHelper with DbTestBase {

  val reCaptchaConfig = CaptchaConfig(checkOn = false, "", "", "")
  val testDb: MySQLProfile.backend.Database = Database.forConfig("tezos-db")
  val emails2SendRepo = new Emails2SendRepository
  val email2SendService = new Emails2SendService(emails2SendRepo, testDb)
  val messageRoute: Route = new MessageRoutes(email2SendService, system.log, reCaptchaConfig).routes

  override def beforeAll(): Unit = {
    recreateTables()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    testDb.close()
  }

  "message endpoint" should {
    "validate dto before storing" in {
      val invalidDto = SendEmailDTO("", "", "", "")
      val expectedErrorsList1 = List("Name field is empty","Phone field is empty","Email field is empty","Content fields is empty")
      checkValidationErrorsWithExpected(invalidDto, expectedErrorsList1)

      val dtoInvalidPhoneAndEmail = SendEmailDTO("name", "+7777777777777777777777", "email@@@gmail.com", "whatsup man")
      val expectedErrorsList2 = List("Invalid phone number +7777777777777777777777","Invalid email email@@@gmail.com")
      checkValidationErrorsWithExpected(dtoInvalidPhoneAndEmail, expectedErrorsList2)

      val dtoNameAndEmailToLong = SendEmailDTO("123456789012345678901234567890123456789012345678901234567890123456789012345678" +
        "90123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678" +
        "901234567890123456789012345678901234567890123456789012345678901234567890",
        "+77777777777",
        "123456789012345678901234567890123456789012345678901234567890123456789012345678" +
          "90123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678" +
          "901234567890123456789012345678901234567890123456789012345678901234567890@gmail.com",
        "whatsup man")
      val expectedErrorsList3 = List("Field name is to long, max length - 255", "Field email is to long, max length - 255")
      checkValidationErrorsWithExpected(dtoNameAndEmailToLong, expectedErrorsList3)

      val tableActual = getAllSendEmailsFromDb
      tableActual.isEmpty shouldBe true
    }

    "save proper dto" in {
      val validDto = SendEmailDTO("name", "+77072123434", "email@gmail.com", "I wanna pizza")
      Post("/message", validDto) ~> messageRoute ~> check {
        status shouldBe StatusCodes.OK
      }
      val tableActual = getAllSendEmailsFromDb

      tableActual.headOption.isEmpty shouldBe false
      val addedRecord = tableActual.head
      addedRecord.name    shouldBe "name"
      addedRecord.phone   shouldBe "+77072123434"
      addedRecord.email   shouldBe "email@gmail.com"
      addedRecord.content shouldBe "I wanna pizza"
    }

  }

  def checkValidationErrorsWithExpected(dto: SendEmailDTO, expectedErrors: List[String]): Assertion = {
    Post("/message", dto) ~> messageRoute ~> check {
      status shouldBe StatusCodes.BadRequest
      responseAs[Errors].errors should contain theSameElementsAs expectedErrors
    }
  }

  def getAllSendEmailsFromDb: Seq[SendEmailModel] = {
    val queryFuture = testDb.run(Emails2SendTable.emails2Send.result)
    Await.result(queryFuture, 1 second)
  }

}
