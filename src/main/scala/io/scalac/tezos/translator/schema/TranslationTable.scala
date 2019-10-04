package io.scalac.tezos.translator.schema

import io.scalac.tezos.translator.model.{Translation, TranslationDomainModel}
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._

object TranslationTable {

  val translations = TableQuery[TranslationTable]

}

class TranslationTable(tag: Tag) extends Table[TranslationDomainModel](tag, "translations") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def from = column[Translation.From]("from", O.SqlType("varchar(20)"))

  def source = column[String]("source")

  def translation = column[String]("translation")

  def createdAt = column[DateTime]("created_at", slick.sql.SqlProfile.ColumnOption.NotNull, O.SqlType("TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)"))

  def * = (id.?, from, source, translation, createdAt) <> (TranslationDomainModel.tupled, TranslationDomainModel.unapply)

}
