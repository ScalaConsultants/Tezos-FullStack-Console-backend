package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.model.SendEmailModel
import io.scalac.tezos.translator.schema.Emails2SendTable
import slick.dbio.Effect
import slick.jdbc.MySQLProfile.api._
import slick.sql.FixedSqlAction
import scala.concurrent.ExecutionContext

class Emails2SendRepository(implicit ec: ExecutionContext) {

  protected val emails2Send = Emails2SendTable.emails2Send

  def getEmails2Send(batchSize: Int): DBIO[Seq[SendEmailModel]] =
    emails2Send
      .take(batchSize)
      .result

  def removeEmail2Send(id: Long): FixedSqlAction[Int, NoStream, Effect.Write] =
    emails2Send
      .filter(_.id === id)
      .delete

}
