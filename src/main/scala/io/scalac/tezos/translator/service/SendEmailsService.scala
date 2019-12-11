package io.scalac.tezos.translator.service

import akka.event.LoggingAdapter
import courier.{Envelope, Mailer, Text}
import io.scalac.tezos.translator.config.{Configuration, EmailConfiguration}
import io.scalac.tezos.translator.model._
import javax.mail.internet.InternetAddress

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SendEmailsService {
  def getEmailsToSend: Future[Seq[SendEmail]]

  def sendEmails(implicit ec: ExecutionContext): Future[Unit] =
    for {
      emailsToSend    <-  getEmailsToSend
      emailsToSendF   =   emailsToSend.map(sendSingleMail)
      _               <-  Future.sequence(emailsToSendF)
    } yield ()

  def sendSingleMail(sendEmailModel: SendEmail): Future[Unit]
}

class SendEmailsServiceImpl(
  service: Emails2SendService,
  batchSize: Int,
  mailer: Mailer,
  log: LoggingAdapter,
  config: EmailConfiguration
)(implicit ec: ExecutionContext) extends SendEmailsService {
  override def getEmailsToSend: Future[Seq[SendEmail]] = service.getEmails2Send(batchSize)

  override def sendSingleMail(sendEmailModel: SendEmail): Future[Unit] = {
    val emailUid = sendEmailModel.uid.value

    val send =
      for {
        addressFrom   <-  Future.fromTry(strictInternetAddress(config.user))
        addressTo     <-  Future.fromTry(getInternetAddress(sendEmailModel.to))
        _             <-  mailer(
                            Envelope
                              .from(addressFrom)
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
      } yield ()

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

    send.flatMap(_ => delete).recover { case _ => ()}
  }

  private def getInternetAddress(email: EmailAddress): Try[InternetAddress] = {
    val emailStr = email match {
      case AdminEmail => config.receiver
      case UserEmail(v) => v
    }

    strictInternetAddress(emailStr)
  }

  private def strictInternetAddress(s: String): Try[InternetAddress] =
    Try(new InternetAddress(s, true))

}

object SendEmailsServiceImpl {
  def apply(
    service: Emails2SendService,
    log: LoggingAdapter,
    configuration: Configuration
  )(implicit ec: ExecutionContext): SendEmailsServiceImpl = {
    val emailConfig = configuration.email

    val mailer: Mailer = Mailer(emailConfig.host, emailConfig.port)
      .auth(emailConfig.auth)
      .as(emailConfig.user, emailConfig.pass)
      .startTls(emailConfig.startTls)()

    new SendEmailsServiceImpl(service, configuration.cron.cronBatchSize, mailer, log, emailConfig)
  }
}
