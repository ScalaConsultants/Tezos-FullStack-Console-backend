package io.scalac.tezos.translator

import io.scalac.tezos.translator.model.{Translation, TranslationDomainModel}
import io.scalac.tezos.translator.schema.TranslationTable
import slick.jdbc.MySQLProfile.api._
import io.scalac.tezos.translator.schema._

class TranslationRepository {

  def add(translation: TranslationDomainModel): DBIO[Int] =
    TranslationTable.translations += translation

  def list(fromFilter: Option[Translation.From], limit: Int): DBIO[Seq[TranslationDomainModel]] =
    TranslationTable.translations
    .filterOpt(fromFilter)(_.from === _ )
    .sortBy(_.createdAt.desc)
    .take(limit)
    .result

}
