package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.testkit.TestKit
import com.icegreen.greenmail.util.{GreenMail, GreenMailUtil, ServerSetupTest}
import io.scalac.tezos.translator.actor.EmailSender
import io.scalac.tezos.translator.config.{Configuration, CronConfiguration, EmailConfiguration}
import io.scalac.tezos.translator.model.{EmailAddress, SendEmail}
import io.scalac.tezos.translator.repository.Emails2SendRepository
import io.scalac.tezos.translator.routes.dto.SendEmailRoutesDto
import io.scalac.tezos.translator.service.{Emails2SendService, SendEmailsServiceImpl}
import javax.mail.internet.MimeMessage
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps

//noinspection TypeAnnotation
class EmailSenderSpec
  extends TestKit(ActorSystem("MySpec"))
  with WordSpecLike
  with ScalaFutures
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach {
    override implicit val patienceConfig: PatienceConfig = PatienceConfig(30 seconds)

    override def beforeAll(): Unit = {
      DbTestBase.recreateTables()
      greenMail.setUser(testMailUser, testMailPass)
      greenMail.start()
    }

    override def afterAll: Unit = {
      greenMail.stop()
      TestKit.shutdownActorSystem(system)
    }

    val testDb = DbTestBase.db

    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val log: LoggingAdapter = system.log
    val emails2SendRepo = new Emails2SendRepository
    val email2SendService = new Emails2SendService(emails2SendRepo, testDb)

    val greenMail = new GreenMail(ServerSetupTest.SMTP)
    val testMailUser = "sender@mail.some"
    val testMailPass = "6131Zz$*n6z2"
    val adminEmail = EmailAddress.fromString("tezos-console-admin@scalac.io").get

    val testCronConfig = CronConfiguration(cronTaskInterval = 3 seconds)
    val testEmailConfig = EmailConfiguration("localhost", 3025, auth = true, testMailUser, testMailPass, receiver = adminEmail.toString)
    val testConfig = Configuration(email = testEmailConfig, cron = testCronConfig)

    override def beforeEach(): Unit = greenMail.purgeEmailFromAllMailboxes()

    "Email sender" should {
      val testName = "testName-tezostests"
      val testPhone = "+79025680396"
      val testMail = "some-tezostests@scalac.io"
      val testContent = "some content"

      val newEmail2Send = SendEmail.fromSendEmailRoutesDto(SendEmailRoutesDto(testName, testPhone, testMail, testContent), adminEmail)
      val emailSenderService = SendEmailsServiceImpl(email2SendService, log, testConfig.email, testConfig.cron).get
      val cronTask = EmailSender(emailSenderService, testCronConfig)

      "send emails" in {
        val addMail: Future[Int] = email2SendService.addNewEmail2Send(newEmail2Send)

        whenReady(addMail) { _ shouldBe 1 }

        whenReady(getNextEmail) { message =>
          message shouldBe 'defined

          val sender = message.get.getFrom.headOption.map(_.toString)
          sender shouldBe Some(testMailUser)

          val recipients = message.get.getAllRecipients.map(_.toString)
          recipients.length shouldBe 1
          recipients.headOption shouldBe Some(adminEmail.toString)

          val body = GreenMailUtil.getBody(message.get).replaceAll("\r", "")

          body shouldBe
            s"""
               |name: $testName
               |phone: $testPhone
               |email: $testMail
               |content: $testContent""".stripMargin
        }


        val dbState: Future[Seq[SendEmail]] =
          Future(Thread.sleep(5000)) // this is to give time for EmailSender actor to finish db deletion
            .flatMap(_ => email2SendService.getEmails2Send(10))

        whenReady(dbState) { _ shouldBe 'empty }

        cronTask.cancel()
      }

      "not send email when address is incorrect" in {
        val newEmail2Send = SendEmail.fromSendEmailRoutesDto(SendEmailRoutesDto(testName, testPhone, "xxx", testContent), adminEmail)
        val addMail: Future[Int] = email2SendService.addNewEmail2Send(newEmail2Send)

        whenReady(addMail) { _ shouldBe 1 }
        whenReady(email2SendService.getEmails2Send(10)) { _.length shouldBe 1 }

        val ex = intercept[Exception](getNextEmail.futureValue)
        ex.getMessage should include("No email was received")
      }

    }

  private def getNextEmail: Future[Option[MimeMessage]] =
    Future(greenMail.waitForIncomingEmail(20000L, 1))
      .flatMap {
        case false  =>  Future.failed(new Exception("No email was received"))
        case true   =>  Future.successful(())
      }
      .map(_ => greenMail.getReceivedMessages.headOption)

}
