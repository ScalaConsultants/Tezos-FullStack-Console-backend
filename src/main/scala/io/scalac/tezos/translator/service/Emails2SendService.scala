package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.model.{SendEmailDTO, SendEmailModel}
import io.scalac.tezos.translator.repository.Emails2SendRepository
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

class Emails2SendService(repository: Emails2SendRepository, db: Database) {

  def addNewEmail2Send(validatedDTO: SendEmailDTO): Future[Int] = {
    db.run(
      sqlu"""insert into emails2send (name, phone, email, content) values
            (${validatedDTO.name}, ${validatedDTO.phone}, ${validatedDTO.email}, ${validatedDTO.content})"""
    )
  }

  def getEmails2Send(batchSize: Int): Future[Seq[SendEmailModel]] = {
    db.run(
      repository.getEmails2Send(batchSize)
    )
  }

  def removeSentMessage(id: Long): Future[Int] = {
    db.run(
      repository.removeEmail2Send(id)
    )
  }

}
