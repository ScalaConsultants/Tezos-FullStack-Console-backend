package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineMV
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.types.ContactData.{ Content, EmailReq, EmailS, Name, NameReq, Phone, PhoneReq }
import io.scalac.tezos.translator.model.{ ContactFormContent, EmailAddress, FullContact, SendEmail }
import io.scalac.tezos.translator.repository.Emails2SendRepository
import io.scalac.tezos.translator.repository.dto.SendEmailDbDto
import io.scalac.tezos.translator.routes.MessageRoutes
import io.scalac.tezos.translator.routes.dto.SendEmailRoutesDto
import io.scalac.tezos.translator.routes.dto.DTO.Errors
import io.scalac.tezos.translator.schema.Emails2SendTable
import io.scalac.tezos.translator.service.Emails2SendService
import org.scalatest.{ Assertion, BeforeAndAfterEach, Matchers, WordSpec }
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

//noinspection TypeAnnotation
class MessageSpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

  val messageEndpoint     = "/v1/message"
  val testDb              = DbTestBase.db
  val reCaptchaConfig     = CaptchaConfig()
  val emails2SendRepo     = new Emails2SendRepository
  val email2SendService   = new Emails2SendService(emails2SendRepo, testDb)
  val adminEmail          = EmailAddress.fromString("tezos-console-admin@service.com").get
  val messageRoute: Route = new MessageRoutes(email2SendService, system.log, reCaptchaConfig, adminEmail).routes

  override def beforeEach(): Unit = DbTestBase.recreateTables()

  private def checkValidationErrorsWithExpected(dto: SendEmailRoutesDto, expectedErrors: List[String]): Assertion =
    Post(messageEndpoint, dto) ~> messageRoute ~> check {
      status shouldBe StatusCodes.BadRequest
      responseAs[Errors].errors should contain theSameElementsAs expectedErrors
    }

  private def getAllSendEmailsFromDb: Seq[SendEmailDbDto] = {
    val queryFuture = testDb.run(Emails2SendTable.emails2Send.result)
    Await.result(queryFuture, 1 second)
  }

  "message endpoint" should {
    "validate dto before storing" in {
      val invalidDto          = SendEmailRoutesDto(Name(refineMV[NameReq]("Dude")), None, None, Content(refineMV[NonEmpty]("some content")))
      val expectedErrorsList1 = List("invalid email, phone - At least one field should be filled")
      checkValidationErrorsWithExpected(invalidDto, expectedErrorsList1)

      val tableActual = getAllSendEmailsFromDb
      tableActual.isEmpty shouldBe true
    }

    "save proper dto" in {
      val validDto = SendEmailRoutesDto(
         Name(refineMV[NameReq]("name")),
         Some(Phone(refineMV[PhoneReq]("+77072123434"))),
         Some(EmailS(refineMV[EmailReq]("email@gmail.com"))),
         Content(refineMV[NonEmpty]("I wanna pizza"))
      )
      Post(messageEndpoint, validDto) ~> messageRoute ~> check {
        status shouldBe StatusCodes.OK
      }
      val tableActual = getAllSendEmailsFromDb

      tableActual.headOption.isEmpty shouldBe false
      val addedRecord         = tableActual.head
      val maybeSendEmailModel = SendEmail.fromSendEmailDbDto(addedRecord)
      maybeSendEmailModel shouldBe a[Success[_]]

      val sendEmailModel = maybeSendEmailModel.get
      sendEmailModel.to shouldBe adminEmail

      sendEmailModel.subject shouldBe "Contact request"

      sendEmailModel.content shouldBe a[ContactFormContent]

      val content = sendEmailModel.content.asInstanceOf[ContactFormContent]

      content.name.toString shouldBe "name"
      content.contact shouldBe FullContact(Phone(refineMV[PhoneReq]("+77072123434")), EmailAddress.fromString("email@gmail.com").get)
      content.content.toString shouldBe "I wanna pizza"
    }
  }
}
