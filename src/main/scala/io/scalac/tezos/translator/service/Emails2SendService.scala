package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.model.DTO.SendEmailDTO
import io.scalac.tezos.translator.repository.Emails2SendRepository
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

class Emails2SendService(repository: Emails2SendRepository, db: Database) {

  def addNewEmail2Send(validatedDTO: SendEmailDTO): Future[Int] = {
    db.run(
      sqlu"""insert into emails2send (name, phone, email, in_sending) values (${validatedDTO.name}, ${validatedDTO.phone}, ${validatedDTO.email}, false)"""
    )
  }

}
