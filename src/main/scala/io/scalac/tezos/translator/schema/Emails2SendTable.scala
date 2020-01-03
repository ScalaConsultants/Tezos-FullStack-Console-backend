package io.scalac.tezos.translator.schema

import java.sql.Timestamp

import io.scalac.tezos.translator.model.types.UUIDs.SendEmailId
import io.scalac.tezos.translator.repository.dto.SendEmailDbDto
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ ProvenShape, Tag }
import slick.sql.SqlProfile.ColumnOption.NotNull

object Emails2SendTable {

  val emails2Send = TableQuery[Emails2SendTable]

}

class Emails2SendTable(tag: Tag) extends Table[SendEmailDbDto](tag, "emails2send") {

  override def * : ProvenShape[SendEmailDbDto] =
    (uid, to, subject, content, createdAt) <> ((SendEmailDbDto.apply _).tupled, SendEmailDbDto.unapply)

  def uid: Rep[SendEmailId] = column[SendEmailId]("uid", NotNull, O.Unique, O.SqlType("VARCHAR(36)"))

  def to: Rep[String] = column[String]("to", NotNull, O.SqlType("TEXT"))

  def subject: Rep[String] = column[String]("subject", NotNull, O.SqlType("TEXT"))

  def content: Rep[String] = column[String]("content", NotNull, O.SqlType("TEXT"))

  def createdAt: Rep[Timestamp] =
    column[Timestamp]("created_at", NotNull, O.SqlType("TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)"))

}
