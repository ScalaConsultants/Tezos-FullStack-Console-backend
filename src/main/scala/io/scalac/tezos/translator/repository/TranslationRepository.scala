package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.model.{Translation, TranslationDomainModel}
import io.scalac.tezos.translator.schema._
import slick.jdbc.MySQLProfile.api._

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
