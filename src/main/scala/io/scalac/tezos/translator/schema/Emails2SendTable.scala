package io.scalac.tezos.translator.schema

import java.sql.Timestamp

import io.scalac.tezos.translator.repository.dto.SendEmailDbDto
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Tag}
import slick.sql.SqlProfile.ColumnOption.NotNull

object Emails2SendTable {

  val emails2Send = TableQuery[Emails2SendTable]

}

class Emails2SendTable(tag: Tag) extends Table[SendEmailDbDto](tag, "emails2send") {

  def uid: Rep[String] = column[String]("uid", NotNull, O.Unique, O.SqlType("VARCHAR(36)"))

  def name: Rep[String] = column[String]("name", NotNull, O.SqlType("TEXT"))

  def phone: Rep[String] = column[String]("phone", NotNull, O.SqlType("TEXT"))

  def email: Rep[String] = column[String]("email", NotNull, O.SqlType("TEXT"))

  def content: Rep[String] = column[String]("content", NotNull, O.SqlType("TEXT"))

  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at", NotNull, O.SqlType("TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)"))

  override def * : ProvenShape[SendEmailDbDto] = (uid, name, phone, email, content, createdAt) <> ((SendEmailDbDto.apply _).tupled, SendEmailDbDto.unapply)

}
