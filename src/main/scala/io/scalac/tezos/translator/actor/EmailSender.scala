package io.scalac.tezos.translator.actor

import akka.actor.{Actor, ActorSystem, Cancellable, Props}
import akka.event.LoggingAdapter
import courier._
import io.scalac.tezos.translator.actor.EmailSender.SendEmails
import io.scalac.tezos.translator.config.{Configuration, EmailConfiguration}
import io.scalac.tezos.translator.model.SendEmailModel
import io.scalac.tezos.translator.service.Emails2SendService

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

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

  protected def sendEmails: Future[Unit] = {
    service
      .getEmails2Send(batchSize)
      .map(_.foreach(sendSingleMail))
  }

  protected def sendSingleMail(sendEmailModel: SendEmailModel): Future[Unit] =
    mailer(
      Envelope
        .from(config.user.addr)
        .to(config.receiver.addr)
        .subject(config.subjectPrefix + s" from:${sendEmailModel.name}")
        .content(textMessageFromSendEmailModel(sendEmailModel))
    ).flatMap { _ =>
      service
        .removeSentMessage(sendEmailModel.id)
        .map(_ => log.debug(s"Message sent - $sendEmailModel"))
        .recover { case err => log.error(s"Can't remove sent message from db - $sendEmailModel, error - $err") }
    }.recover {
      case err => log.error(s"Can't send message - $sendEmailModel, error - $err")
    }

  protected def textMessageFromSendEmailModel(mail: SendEmailModel): Text =
    Text(
      s"""
         |name: ${mail.name}
         |phone: ${mail.phone}
         |email: ${mail.email}
         |content: ${mail.content}
         |""".stripMargin
    )

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
