package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.model.types.UUIDs.SendEmailId
import io.scalac.tezos.translator.repository.dto.SendEmailDbDto
import io.scalac.tezos.translator.schema.Emails2SendTable
import slick.jdbc.PostgresProfile.api._

class Emails2SendRepository() {

  protected val emails2Send = Emails2SendTable.emails2Send

  def add(sendEmail: SendEmailDbDto): DBIO[Int] =
    emails2Send += sendEmail

  def getEmails2Send(batchSize: Int): DBIO[Seq[SendEmailDbDto]] =
    emails2Send
      .take(batchSize)
      .result

  def removeEmail2Send(uid: SendEmailId): DBIO[Int] =
    emails2Send
      .filter(_.uid === uid)
      .delete

}
