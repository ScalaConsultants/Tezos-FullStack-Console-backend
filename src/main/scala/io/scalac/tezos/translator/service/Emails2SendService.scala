package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.model.{SendEmail, Uid}
import io.scalac.tezos.translator.repository.Emails2SendRepository
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class Emails2SendService(repository: Emails2SendRepository, db: Database) {

  def addNewEmail2Send(validatedDTO: SendEmail): Future[Int] =
    db.run(
      repository.add(validatedDTO.toDbDTO)
    )

  def getEmails2Send(batchSize: Int)(implicit ec: ExecutionContext): Future[Seq[SendEmail]] =
    for {
      entriesDto  <-  db.run(repository.getEmails2Send(batchSize))
      entriesFSeq =   entriesDto.map(e => Future.fromTry(SendEmail.fromDbDto(e)))
      entries     <-  Future.sequence(entriesFSeq)
    } yield entries


  def removeSentMessage(uid: Uid): Future[Int] = {
    db.run(
      repository.removeEmail2Send(uid.value)
    )
  }

}
