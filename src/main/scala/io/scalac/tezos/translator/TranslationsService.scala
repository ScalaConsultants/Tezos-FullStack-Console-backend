package io.scalac.tezos.translator

import io.scalac.tezos.translator.model.{Translation, TranslationDomainModel}
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class TranslationsService(implicit repository: TranslationRepository, db: Database) {

  def addTranslation(from: Translation.From, source: String, translation: String)(implicit ec: ExecutionContext): Future[Any] =
    db.run {
      repository.add(
        TranslationDomainModel(id = None, from = from, source = source, translation = translation, createdAt = DateTime.now)
      )
    }

  def list(fromFilter: Option[Translation.From], limit: Int): Future[Seq[TranslationDomainModel]] =
    db.run(repository.list(fromFilter, limit))

}
