package io.scalac.tezos.translator.actor

import akka.actor.{Actor, ActorSystem, Cancellable, Props}
import io.scalac.tezos.translator.actor.EmailSender.SendEmails
import io.scalac.tezos.translator.service.Emails2SendService
import courier._
import akka.event.LoggingAdapter
import io.scalac.tezos.translator.config.{Configuration, EmailConfiguration}
import io.scalac.tezos.translator.model.SendEmailModel
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class EmailSender(service: Emails2SendService,
                  config: EmailConfiguration,
                  batchSize: Int,
                  log: LoggingAdapter) extends Actor {

  implicit val ec: ExecutionContextExecutor = context.dispatcher
  val mailer: Mailer = Mailer(config.host, config.port)
    .auth(config.auth)
    .as(config.user, config.pass)
    .startTls(config.startTls)()

  override def receive: Receive = {
    case SendEmails => sendEmails
  }

  def sendEmails: Future[Unit] = {
    service
      .getEmails2Send(batchSize)
      .map(_.foreach(sendSingleMail))
  }

  def sendSingleMail(sendEmailModel: SendEmailModel): Unit = {
    mailer(
      Envelope
        .from(config.user.addr)
        .to(config.receiver.addr)
        .subject(config.subjectPrefix + s" from:${sendEmailModel.name}")
        .content(textMessageFromSendEmailModer(sendEmailModel))
    ).onComplete {
      case Failure(err) =>
        log.error(s"Can't send message - $sendEmailModel, error - $err")
      case Success(_)   =>
        service.removeSentMessage(sendEmailModel.id)
        log.debug(s"Message sent - $sendEmailModel")
    }
  }

  def textMessageFromSendEmailModer(mail: SendEmailModel): Text =
    Text(
      s"""
         |name: ${mail.name}
         |phone: ${mail.phone}
         |content: ${mail.content}
         |""".stripMargin
    )

}

object EmailSender {

  def apply(service: Emails2SendService, config: Configuration, log: LoggingAdapter)(implicit ac: ActorSystem): Cancellable = {
    import ac.dispatcher
    val cronConfig = config.cron
    val actor = ac.actorOf(Props(new EmailSender(service, config.email, cronConfig.cronBatchSize, log)))
    ac.scheduler.schedule(cronConfig.startDelay, cronConfig.cronTaskInterval, actor, SendEmails)
  }

  case object SendEmails

}
