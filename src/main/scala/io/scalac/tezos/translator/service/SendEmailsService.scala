package io.scalac.tezos.translator.service

import akka.event.LoggingAdapter
import courier.{Envelope, Mailer, Text}
import io.scalac.tezos.translator.config.{CronConfiguration, EmailConfiguration}
import io.scalac.tezos.translator.model._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SendEmailsService {
  def getEmailsToSend: Future[Seq[SendEmail]]

  def sendEmails(implicit ec: ExecutionContext): Future[Unit] =
    for {
      emailsToSend <- getEmailsToSend
      emailsToSendF = emailsToSend.map(sendSingleMail)
      _ <- Future.sequence(emailsToSendF)
    } yield ()

  def sendSingleMail(sendEmailModel: SendEmail): Future[Unit]
}

class SendEmailsServiceImpl(
  service: Emails2SendService,
  batchSize: Int,
  mailer: Mailer,
  log: LoggingAdapter,
  serviceEmail: EmailAddress
)(implicit ec: ExecutionContext)
    extends SendEmailsService {
  override def getEmailsToSend: Future[Seq[SendEmail]] = service.getEmails2Send(batchSize)

  override def sendSingleMail(sendEmailModel: SendEmail): Future[Unit] = {
    val emailUid = sendEmailModel.uid.value
    val addressTo = sendEmailModel.to.value

    val send =
      mailer(
          Envelope
            .from(serviceEmail.value)
            .to(addressTo)
            .subject(sendEmailModel.subject)
            .content(Text(EmailContent.toPrettyString(sendEmailModel.content)))
      ).transform {
        case Success(v) =>
          log.debug(s"Message sent - $sendEmailModel")
          Success(v)

        case Failure(ex) =>
          log.error(s"Can't send message - $emailUid, error - ${ex.getMessage}")
          Failure(ex)
      }

    val delete =
      service
        .removeSentMessage(sendEmailModel.uid)
        .transform {
          case Success(0) =>
            log.warning(s"There is no message $emailUid, to delete")
            Success(())

          case Success(_) =>
            log.debug(s"Message $sendEmailModel deleted")
            Success(())

          case Failure(ex) =>
            log.error(s"Cannot delete message - $emailUid, error - ${ex.getMessage}")
            Failure(ex)
        }

    send.flatMap(_ => delete).recover { case _ => () }
  }

}

object SendEmailsServiceImpl {

  def apply(
    service: Emails2SendService,
    log: LoggingAdapter,
    emailConfig: EmailConfiguration,
    cronConfig: CronConfiguration
  )(implicit ec: ExecutionContext
  ): Try[SendEmailsServiceImpl] =
    EmailAddress.fromString(emailConfig.user).map { serviceEmail =>
      val mailer: Mailer = Mailer(emailConfig.host, emailConfig.port)
        .auth(emailConfig.auth)
        .as(serviceEmail.toString, emailConfig.pass)
        .startTls(emailConfig.startTls)()

      new SendEmailsServiceImpl(service, cronConfig.cronBatchSize, mailer, log, serviceEmail)
    }
}
