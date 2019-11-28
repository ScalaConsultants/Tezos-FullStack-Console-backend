package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.testkit.TestKit
import com.dimafeng.testcontainers.{ForEachTestContainer, MySQLContainer}
import com.icegreen.greenmail.util.{GreenMail, GreenMailUtil, ServerSetupTest}
import io.scalac.tezos.translator.actor.EmailSender
import io.scalac.tezos.translator.config.{Configuration, CronConfiguration, EmailConfiguration}
import io.scalac.tezos.translator.model.SendEmail
import io.scalac.tezos.translator.repository.Emails2SendRepository
import io.scalac.tezos.translator.routes.JsonHelper
import io.scalac.tezos.translator.routes.dto.SendEmailRoutesDto
import io.scalac.tezos.translator.service.Emails2SendService
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import slick.jdbc.MySQLProfile

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps

//noinspection TypeAnnotation
class EmailSenderSpec
  extends TestKit(ActorSystem("MySpec"))
  with WordSpecLike
  with ScalaFutures
  with Matchers
  with JsonHelper
  with ForEachTestContainer {

    implicit val ec: ExecutionContextExecutor = system.dispatcher
    override lazy val container = MySQLContainer(mysqlImageVersion = DbTestBase.mySqlVersion)
    override implicit val patienceConfig: PatienceConfig = PatienceConfig(10 seconds)

    private trait DatabaseFixture extends DbTestBase {
      val testDb: MySQLProfile.backend.Database = DbTestBase.dbFromContainer(container)

      val email2SendService = new Emails2SendService(emails2SendRepo, testDb)

      recreateTables()
      greenMail.setUser(testMailUser, testMailPass)
      greenMail.start()
    }

    val log: LoggingAdapter = system.log
    val emails2SendRepo = new Emails2SendRepository
    val greenMail = new GreenMail(ServerSetupTest.SMTP)
    val testMailUser = "sender-tezostests@scalac.io"
    val testMailPass = "6131Zz$*n6z2"
    val testReceiver = "testrec-tezostests@scalac.io"

    val testCronConfig = CronConfiguration(cronTaskInterval = 1 seconds)
    val testEmailConfig = EmailConfiguration("localhost", 3025, auth = true, testMailUser, testMailPass, receiver = testReceiver)
    val testConfig = Configuration(email = testEmailConfig, cron = testCronConfig)

    "Email sender" should {
      "send emails" in new DatabaseFixture {

        val testName = "testName-tezostests"
        val testPhone = "+79025680396"
        val testMail = "some-tezostests@scalac.io"
        val testContent = "some content"

        val newEmail2Send = SendEmailRoutesDto(testName, testPhone, testMail, testContent).toDomain
        val cronTask = EmailSender(email2SendService, testConfig, log)

        whenReady(email2SendService.getEmails2Send(10)) { _ shouldBe 'empty }

        val addMail: Future[Int] = email2SendService.addNewEmail2Send(newEmail2Send)

        whenReady(addMail) { _ shouldBe 1 }

        val messageF = Future(greenMail.waitForIncomingEmail(8000L, 1))
          .flatMap {
            case false  =>  Future.failed(new Exception("No email was received"))
            case true   =>  Future.successful(())
          }
          .map(_ => greenMail.getReceivedMessages.headOption)

        whenReady(messageF) { message =>
          message shouldBe a[Some[_]]

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

      greenMail.stop()
    }

}
