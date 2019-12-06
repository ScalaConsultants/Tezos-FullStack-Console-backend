package io.scalac.tezos.translator.actor

import akka.actor.{Actor, ActorSystem, Cancellable, Props}
import akka.event.LoggingAdapter
import courier._
import io.scalac.tezos.translator.actor.EmailSender.SendEmails
import io.scalac.tezos.translator.config.{Configuration, EmailConfiguration}
import io.scalac.tezos.translator.model.SendEmail
import io.scalac.tezos.translator.model.SendEmail.{AdminEmail, Content, EmailAddress, MmtServiceEmail, UserEmail}
import io.scalac.tezos.translator.service.Emails2SendService
import javax.mail.internet.InternetAddress

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Try

class EmailSender(service: Emails2SendService,
                  config: EmailConfiguration,
                  batchSize: Int,
                  log: LoggingAdapter)(implicit ec: ExecutionContext) extends Actor {

  protected val mailer: Mailer = Mailer(config.host, config.port)
    .auth(config.auth)
    .as(config.user, config.pass)
    .startTls(config.startTls)()

  override def receive: Receive = {
    case SendEmails => sendEmails
  }

  protected def sendEmails: Future[Unit] =
    for {
      emailsToSend    <-  service.getEmails2Send(batchSize)
      emailsToSendF   =   emailsToSend.map(sendSingleMail)
      _               <-  Future.sequence(emailsToSendF)
    } yield ()

  protected def sendSingleMail(sendEmailModel: SendEmail): Future[Unit] = {
    val sendAndDelete = for {
      addressFrom   <-  Future.fromTry(getInternetAddress(sendEmailModel.from))
      addressTo     <-  Future.fromTry(getInternetAddress(sendEmailModel.to))
      _             <-  mailer(
                          Envelope
                            .from(addressFrom)
                            .to(addressTo)
                            .subject(sendEmailModel.subject)
                            .content(Text(Content.toPrettyString(sendEmailModel.content)))
                        )
      _             <-  service
                          .removeSentMessage(sendEmailModel.uid)
                          .map(_ => log.debug(s"Message sent - $sendEmailModel"))
                          .recover { case err => log.error(s"Can't remove sent message from db - $sendEmailModel, error - $err") }
    } yield ()

    sendAndDelete.recover {
      case err => log.error(s"Can't send message - $sendEmailModel, error - $err")
    }
  }

  private def getInternetAddress(email: EmailAddress): Try[InternetAddress] = {
    val emailStr = email match {
      case AdminEmail => config.receiver
      case MmtServiceEmail => config.user
      case UserEmail(v) => v
    }

    Try(new InternetAddress(emailStr, true))
  }

}

object EmailSender {

  def apply(service: Emails2SendService, config: Configuration, log: LoggingAdapter)(implicit ac: ActorSystem): Cancellable = {
    implicit val ec: ExecutionContextExecutor = ac.dispatcher
    val cronConfig = config.cron
    val actor = ac
      .actorOf(Props(
        new EmailSender(service, config.email, cronConfig.cronBatchSize, log)
        (ac.dispatchers.lookup("blocking-dispatcher"))
      )
      .withDispatcher("blocking-dispatcher"), "email-sender")
    ac.scheduler.schedule(cronConfig.startDelay, cronConfig.cronTaskInterval, actor, SendEmails)
  }

  case object SendEmails

}
