package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.config.DBUtilityConfiguration
import io.scalac.tezos.translator.model.LibraryEntry.Status
import io.scalac.tezos.translator.model.Uid
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto
import io.scalac.tezos.translator.schema.LibraryTable
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class LibraryRepository(
  config: DBUtilityConfiguration,
  db: Database
)(implicit ec: ExecutionContext) {

  def add(translation: LibraryEntryDbDto): Future[Int] =
    db.run(LibraryTable.library += translation)

  def list(
    status: Option[Status],
    offset: Option[Int],
    limit: Option[Int]
  ): Future[Seq[LibraryEntryDbDto]] =
    db.run {
      LibraryTable.library
        .filterOpt(status) { case (row, s) => row.status === s.value }
        .sortBy(_.createdAt.desc)
        .drop(offset.getOrElse(0))
        .take(limit.getOrElse(config.defaultLimit))
        .result
    }

  def get(uid: Uid): Future[Option[LibraryEntryDbDto]] = db.run {
    LibraryTable.library
      .filter(_.uid === uid.value)
      .take(1)
      .result
      .headOption
  }

  def update(
    uid: Uid,
    libraryEntry: LibraryEntryDbDto
  ): Future[Option[LibraryEntryDbDto]] =
    db.run {
      LibraryTable.library
        .filter(_.uid === uid.value)
        .update(libraryEntry)
        .map {
          case 0 => None
          case _ => Some(libraryEntry)
        }
    }

  def delete(uid: Uid): Future[Int] =
    db.run {
      LibraryTable.library
        .filter(_.uid === uid.value)
        .delete
    }
}
