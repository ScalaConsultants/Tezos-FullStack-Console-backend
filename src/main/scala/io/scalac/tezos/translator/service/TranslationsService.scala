package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.model.{Translation, TranslationDomainModel}
import io.scalac.tezos.translator.repository.TranslationRepository
import org.joda.time.{DateTime, DateTimeZone}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

class TranslationsService(implicit repository: TranslationRepository, db: Database) {

  def addTranslation(from: Translation.From, source: String, translation: String): Future[Int] =
    db.run {
      repository.add(
        TranslationDomainModel(id = None, from = from, source = source, translation = translation, createdAt = DateTime.now(DateTimeZone.UTC))
      )
    }

  def list(fromFilter: Option[Translation.From], limit: Int): Future[Seq[TranslationDomainModel]] =
    db.run(repository.list(fromFilter, limit))

}
