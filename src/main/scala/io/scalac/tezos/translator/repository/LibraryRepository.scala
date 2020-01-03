package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.config.DBUtilityConfiguration
import io.scalac.tezos.translator.model.LibraryEntry.Status
import io.scalac.tezos.translator.model.types.Params.{ Limit, Offset }
import io.scalac.tezos.translator.model.types.UUIDs.LibraryEntryId
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto
import io.scalac.tezos.translator.schema.LibraryTable
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ ExecutionContext, Future }

class LibraryRepository(config: DBUtilityConfiguration, db: Database)(implicit ec: ExecutionContext) {

  def add(translation: LibraryEntryDbDto): Future[Int] =
    db.run(LibraryTable.library += translation)

  def list(status: Option[Status], offset: Option[Offset], limit: Option[Limit]): Future[Seq[LibraryEntryDbDto]] =
    db.run {
      LibraryTable.library
        .filterOpt(status) { case (row, s) => row.status === s.value }
        .sortBy(_.createdAt.desc)
        .drop(offset.fold(0)(_.v.value))
        .take(limit.fold(config.defaultLimit)(_.v.value))
        .result
    }

  def get(uid: LibraryEntryId): Future[Option[LibraryEntryDbDto]] = db.run {
    LibraryTable.library
      .filter(_.uid === uid)
      .take(1)
      .result
      .headOption
  }

  def update(uid: LibraryEntryId, libraryEntry: LibraryEntryDbDto): Future[Option[LibraryEntryDbDto]] =
    db.run {
      LibraryTable.library
        .filter(_.uid === uid)
        .update(libraryEntry)
        .map {
          case 0 => None
          case _ => Some(libraryEntry)
        }
    }

  def delete(uid: LibraryEntryId): Future[Int] =
    db.run {
      LibraryTable.library
        .filter(_.uid === uid)
        .delete
    }
}
