package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.testkit.TestKit
import com.icegreen.greenmail.util.{ GreenMail, GreenMailUtil, ServerSetupTest }
import eu.timepit.refined.collection.NonEmpty
import io.scalac.tezos.translator.config.{ CronConfiguration, EmailConfiguration }
import io.scalac.tezos.translator.model.LibraryEntry.Accepted
import io.scalac.tezos.translator.model.types.ContactData.{ Content, EmailReq, EmailS, Name, NameReq, Phone, PhoneReq }
import io.scalac.tezos.translator.model.{ EmailAddress, SendEmail }
import io.scalac.tezos.translator.repository.Emails2SendRepository
import io.scalac.tezos.translator.routes.dto.{ LibraryEntryRoutesDto, SendEmailRoutesDto }
import io.scalac.tezos.translator.service.{ Emails2SendService, SendEmailsServiceImpl }
import javax.mail.internet.MimeMessage
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Matchers }
import eu.timepit.refined.refineMV
import io.scalac.tezos.translator.model.types.Library.{ Author, Description, Micheline, Michelson, NotEmptyAndNotLong, Title }
import cats.syntax.option._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success
import scala.language.postfixOps

//noinspection TypeAnnotation
class SendEmailServiceSpec
    extends TestKit(ActorSystem("MySpec"))
    with FlatSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with BeforeAndAfterEach {
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(30 seconds)

  val greenMail           = new GreenMail(ServerSetupTest.SMTP)
  val testDb              = DbTestBase.db
  val log: LoggingAdapter = system.log
  val emails2SendRepo     = new Emails2SendRepository
  val email2SendService   = new Emails2SendService(emails2SendRepo, testDb)
  val testMailUser        = "sender@mail.some"
  val testMailPass        = "6131Zz$*n6z2"
  val testAdmin           = "testrec@some.some"
  val testAdminEmail      = unsafeEmailAddress(testAdmin)
  val testCronConfig      = CronConfiguration(cronTaskInterval = 3 seconds)
  val testEmailConfig     = EmailConfiguration("localhost", 3025, auth = true, testMailUser, testMailPass, receiver = testAdmin)
  val emailSenderService  = SendEmailsServiceImpl(email2SendService, log, testEmailConfig, testCronConfig).get

  override def beforeAll(): Unit = {
    greenMail.setUser(testMailUser, testMailPass)
    greenMail.start()
  }

  override def afterAll: Unit = {
    greenMail.stop()
    TestKit.shutdownActorSystem(system)
  }

  override def beforeEach(): Unit = {
    DbTestBase.recreateTables()
    greenMail.purgeEmailFromAllMailboxes()
  }

  behavior of "SendEmailsService.getEmailsToSend"

  it should "get empty list when no emails2send in db" in {
    whenReady(emailSenderService.getEmailsToSend) { _ shouldBe 'empty }
  }

  it should "get all emails list" in new SampleEmails {
    whenReady(insert(email2SendService)) { _ shouldBe Seq(1, 1, 1) }

    val expected: Seq[SendEmail] = toInsert

    whenReady(emailSenderService.getEmailsToSend) { emailsToSend =>
      emailsToSend should contain theSameElementsAs expected
    }
  }

  behavior of "SendEmailsService.sendSingleMail"

  it should "send an email" in {
    val email = SendEmail.statusChange(unsafeEmailAddress("xxx@service.com"), Title(refineMV("translation title")), Accepted)

    whenReady(emailSenderService.sendSingleMail(email)) { _ shouldBe (()) }

    val received = greenMail.getReceivedMessages
    received.length shouldBe 1

    val message = received.headOption

    message shouldBe defined

    val sender = message.get.getFrom.headOption.map(_.toString)
    sender shouldBe Some("sender@mail.some")

    val recipients = message.get.getAllRecipients.map(_.toString)
    recipients.length shouldBe 1
    recipients.headOption shouldBe Some("xxx@service.com")

    val body = GreenMailUtil.getBody(message.get).replaceAll("\r", "")

    body shouldBe """Acceptance status of your translation: "translation title" has changed to: accepted"""
  }

  behavior of "SendEmailsService.sendEmails"

  it should "send all emails from a queue" in new SampleEmails {
    whenReady(insert(email2SendService)) { _ shouldBe Seq(1, 1, 1) }

    whenReady(emailSenderService.sendEmails) { _ shouldBe (()) }

    val received: Map[String, MimeMessage] =
      greenMail.getReceivedMessages.toList.map { m =>
        val subject = m.getSubject

        (subject, m)
      }.toMap

    received.size shouldBe 3

    received.get(e1.subject) shouldBe defined
    val e1SendResult = received(e1.subject)

    val sender1 = e1SendResult.getFrom.headOption.map(_.toString)
    sender1 shouldBe Some("sender@mail.some")

    val recipients1 = e1SendResult.getAllRecipients.map(_.toString)
    recipients1.length shouldBe 1
    recipients1.headOption shouldBe Some("xxx@service.com")

    val body1 = GreenMailUtil.getBody(e1SendResult)

    body1 shouldBe """Acceptance status of your translation: "translation title" has changed to: accepted"""

    received.get(e2.subject) shouldBe defined
    val e2SendResult = received(e2.subject)

    val sender2 = e2SendResult.getFrom.headOption.map(_.toString)
    sender2 shouldBe Some("sender@mail.some")

    val recipients2 = e2SendResult.getAllRecipients.map(_.toString)
    recipients2.length shouldBe 1
    recipients2.headOption shouldBe Some(testAdmin) // this should go to "admin", which is testAdmin in here

    val body2 = GreenMailUtil.getBody(e2SendResult)

    Helper.testFormat(body2) shouldBe
      Helper.testFormat(s"""
                           |name: Dude
                           |phone: 666666666
                           |email: dude@service.com
                           |content: some content""".stripMargin)

    received.get(e3.subject) shouldBe defined
    val e3SendResult = received(e3.subject)

    val sender3 = e3SendResult.getFrom.headOption.map(_.toString)
    sender3 shouldBe Some("sender@mail.some")

    val recipients3 = e3SendResult.getAllRecipients.map(_.toString)
    recipients3.length shouldBe 1
    recipients3.headOption shouldBe Some(testAdmin) // this should go to "admin", which is testAdmin in here

    val body3 = GreenMailUtil.getBody(e3SendResult).replaceAll("\r", "")

    body3 should contain
    s"""
       |Please add my translation to your library:
       |Title: contract name
       |Description: Some description
      """.stripMargin

  }

  private def unsafeEmailAddress(s: String): EmailAddress = {
    val email = EmailAddress.fromString(s)
    email shouldBe a[Success[_]]
    email.get
  }

  private trait SampleEmails {
    val e1: SendEmail = SendEmail.statusChange(unsafeEmailAddress("xxx@service.com"), Title(refineMV("translation title")), Accepted)

    val e2: SendEmail = SendEmail
      .fromSendEmailRoutesDto(
         SendEmailRoutesDto(Name(refineMV[NameReq]("Dude")),
                            Some(Phone(refineMV[PhoneReq]("666666666"))),
                            Some(EmailS(refineMV[EmailReq]("dude@service.com"))),
                            Content(refineMV[NonEmpty]("some content"))),
         testAdminEmail
      )
      .get

    val e3: SendEmail = SendEmail.approvalRequest(
       LibraryEntryRoutesDto(Title(refineMV[NotEmptyAndNotLong]("contract name")),
                             Author(refineMV[NotEmptyAndNotLong]("Thanos")).some,
                             None,
                             Description(refineMV[NotEmptyAndNotLong]("Some description")).some,
                             Micheline(refineMV[NonEmpty]("micheline")),
                             Michelson(refineMV[NonEmpty]("michelson"))),
       testAdminEmail
    )

    val toInsert: Seq[SendEmail] = Seq(e1, e2, e3)

    def insert(service: Emails2SendService, toInsert: Seq[SendEmail] = toInsert): Future[Seq[Int]] =
      Future.sequence(toInsert.map(service.addNewEmail2Send))
  }

}
