package io.scalac.tezos.translator

import io.scalac.tezos.translator.model.{Translation, TranslationDomainModel}
import io.scalac.tezos.translator.schema.TranslationTable
import slick.jdbc.MySQLProfile.api._
import io.scalac.tezos.translator.schema._

import scala.concurrent.ExecutionContext

class TranslationRepository {

  def find(source: String): DBIO[Seq[TranslationDomainModel]] =
    TranslationTable.translations
      .filter(_.source === source)
      .take(1)
      .result

  def add(translation: TranslationDomainModel)(implicit ec: ExecutionContext): DBIOAction[Any, NoStream, Effect.Read with Effect.Write with Effect.Transactional] =
    TranslationTable.translations
      .filter(_.source === translation.source)
      .exists
      .result
      .flatMap {
        exists =>
          if(!exists) {
            TranslationTable.translations += translation
          }
          else {
            DBIO.successful(None)
          }
      }
      .transactionally

  def list(fromFilter: Option[Translation.From], limit: Int): DBIO[Seq[TranslationDomainModel]] =

    TranslationTable.translations
    .filterOpt(fromFilter)(_.from === _ )
    .sortBy(_.createdAt.desc)
    .take(limit)
    .result

}
