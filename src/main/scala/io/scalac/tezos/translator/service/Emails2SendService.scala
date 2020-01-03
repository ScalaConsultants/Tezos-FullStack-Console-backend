package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.model.SendEmail
import io.scalac.tezos.translator.model.types.UUIDs.SendEmailId
import io.scalac.tezos.translator.repository.Emails2SendRepository
import io.scalac.tezos.translator.repository.dto.SendEmailDbDto
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ ExecutionContext, Future }

class Emails2SendService(repository: Emails2SendRepository, db: Database) {

  def addNewEmail2Send(email: SendEmail): Future[Int] =
    db.run(repository.add(SendEmailDbDto.fromDomain(email)))

  def getEmails2Send(batchSize: Int)(implicit ec: ExecutionContext): Future[Seq[SendEmail]] =
    for {
      entriesDto  <- db.run(repository.getEmails2Send(batchSize))
      entriesFSeq = entriesDto.map(e => Future.fromTry(SendEmail.fromSendEmailDbDto(e)))
      entries     <- Future.sequence(entriesFSeq)
    } yield entries

  def removeSentMessage(uid: SendEmailId): Future[Int] =
    db.run(repository.removeEmail2Send(uid))

}
