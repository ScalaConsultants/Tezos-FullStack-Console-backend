package io.scalac.tezos.translator.actor

import akka.actor.{Actor, ActorSystem, Cancellable, Props}
import io.scalac.tezos.translator.actor.EmailSender.SendEmails
import io.scalac.tezos.translator.config.CronConfiguration
import io.scalac.tezos.translator.service.SendEmailsService

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class EmailSender(service: SendEmailsService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case SendEmails =>
      service.sendEmails
      ()
  }
}

object EmailSender {

  def apply(service: SendEmailsService, cronConfig: CronConfiguration)(implicit ac: ActorSystem): Cancellable = {
    implicit val ec: ExecutionContextExecutor = ac.dispatcher
    val actor = ac
      .actorOf(Props(new EmailSender(service)(ac.dispatchers.lookup("blocking-dispatcher")))
                 .withDispatcher("blocking-dispatcher"),
               "email-sender")
    ac.scheduler.schedule(cronConfig.startDelay, cronConfig.cronTaskInterval, actor, SendEmails)
  }

  case object SendEmails
}
