package io.scalac.tezos.translator.schema

import java.sql.Timestamp

import io.scalac.tezos.translator.model.SendEmailDbDTO
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}

object Emails2SendTable {

  val emails2Send = TableQuery[Emails2SendTable]

}

class Emails2SendTable(tag: Tag) extends Table[SendEmailDbDTO](tag, "emails2send") {

  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def uid: Rep[String] = column[String]("uid", O.SqlType("VARCHAR(8) NOT NULL"))

  def name: Rep[String] = column[String]("name", O.SqlType("TINYTEXT NOT NULL"))

  def phone: Rep[String] = column[String]("phone", O.SqlType("TINYTEXT NOT NULL"))

  def email: Rep[String] = column[String]("email", O.SqlType("TINYTEXT NOT NULL"))

  def content: Rep[String] = column[String]("content", O.SqlType("TEXT NOT NULL"))

  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at", slick.sql.SqlProfile.ColumnOption.NotNull, O.SqlType("TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)"))

  override def * : ProvenShape[SendEmailDbDTO] = (id.?, uid, name, phone, email, content, createdAt) <> (SendEmailDbDTO.tupled, SendEmailDbDTO.unapply)

}
