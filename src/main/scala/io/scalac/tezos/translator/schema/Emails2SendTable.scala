package io.scalac.tezos.translator.schema

import io.scalac.tezos.translator.model.SendEmailModel
import org.joda.time.DateTime
import slick.lifted.{ProvenShape, Tag}
import slick.jdbc.MySQLProfile.api._

object Emails2SendTable {

  val emails2Send = TableQuery[Emails2SendTable]

}

class Emails2SendTable(tag: Tag) extends Table[SendEmailModel](tag, "emails2send") {

  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def name: Rep[String] = column[String]("name", O.SqlType("tinytext"))

  def phone: Rep[String] = column[String]("phone")

  def inSending: Rep[Boolean] = column[Boolean]("in_sending")

  def createdAt: Rep[DateTime] = column[DateTime]("created_at", slick.sql.SqlProfile.ColumnOption.NotNull, O.SqlType("TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)"))

  def lastTryToSend: Rep[DateTime] = column[DateTime]("last_try_to_send", slick.sql.SqlProfile.ColumnOption.NotNull, O.SqlType("TIMESTAMP(6) NULL"))

  override def * : ProvenShape[SendEmailModel] = (id, name, phone, inSending, createdAt, lastTryToSend.?) <> (SendEmailModel.tupled, SendEmailModel.unapply)

}
