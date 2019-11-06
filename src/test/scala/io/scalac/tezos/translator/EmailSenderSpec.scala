package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.testkit.TestKit
import com.icegreen.greenmail.util.{GreenMail, GreenMailUtil, ServerSetupTest}
import io.scalac.tezos.translator.actor.EmailSender
import io.scalac.tezos.translator.config.{Configuration, CronConfiguration, EmailConfiguration}
import io.scalac.tezos.translator.model.DTO.SendEmailDTO
import io.scalac.tezos.translator.repository.Emails2SendRepository
import io.scalac.tezos.translator.routes.JsonHelper
import io.scalac.tezos.translator.service.Emails2SendService
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.language.postfixOps

class EmailSenderSpec extends TestKit(ActorSystem("MySpec")) with DbTestBase
  with WordSpecLike with Matchers with BeforeAndAfterAll with JsonHelper with Eventually {

  implicit val ec: ExecutionContextExecutor = system.dispatcher
  val log: LoggingAdapter = system.log
  val testDb: MySQLProfile.backend.Database = Database.forConfig("tezos-db")
  val emails2SendRepo = new Emails2SendRepository
  val email2SendService = new Emails2SendService(emails2SendRepo, testDb)
  val greenMail = new GreenMail(ServerSetupTest.SMTP)
  val testMailUser = "sender@mail.some"
  val testMailPass = "6131Zz$*n6z2"
  val testReceiver = "testrec@some.some"

  val testCronConfig = CronConfiguration(cronTaskInterval = 5 seconds)
  val testEmailConfig = EmailConfiguration("localhost", 3025, auth = true, testMailUser, testMailPass, receiver = testReceiver)
  val testConfig = Configuration(email = testEmailConfig, cron = testCronConfig)

  override def beforeAll(): Unit = {
    recreateTables()
    greenMail.setUser(testMailUser, testMailPass)
    greenMail.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    testDb.close()
    greenMail.stop()
  }

  "Email sender" should {
    "send emails" in {

      val testName    = "testName"
      val testPhone   = "+79025680396"
      val testMail    = "some@some.some"
      val testContent = "some content"

      val newEmail2Send = SendEmailDTO(testName, testPhone, testMail, testContent)

      Await.ready(email2SendService.addNewEmail2Send(newEmail2Send), 1 second)

      val cronTask = EmailSender(email2SendService, testConfig, log)

      greenMail.waitForIncomingEmail(1)
      val message = greenMail.getReceivedMessages.head

      val body = GreenMailUtil.getBody(message).replaceAll("\r", "")

      body shouldBe
        s"""
           |name: $testName
           |phone: $testPhone
           |email: $testMail
           |content: $testContent""".stripMargin

      eventually {
        val afterTestEmails2Send = Await.result(email2SendService.getEmails2Send(10), 1 second)
        afterTestEmails2Send.isEmpty shouldBe true
      }
      cronTask.cancel()
    }
  }

}
