package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.config.DBUtilityConfiguration
import io.scalac.tezos.translator.model.LibraryEntry.Status
import io.scalac.tezos.translator.model.Uid
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto
import io.scalac.tezos.translator.schema.LibraryTable
import slick.jdbc.PostgresProfile.api._

class LibraryRepository(config: DBUtilityConfiguration) {

  def add(translation: LibraryEntryDbDto): DBIO[Int] =
    LibraryTable.library += translation

  def list(status: Option[Status], offset: Option[Int], limit: Option[Int]): DBIO[Seq[LibraryEntryDbDto]] =
    LibraryTable.library.filterOpt(status){ case (row, s) =>  row.status === s.value}
      .sortBy(_.createdAt.desc)
      .drop(offset.getOrElse(0))
      .take(limit.getOrElse(config.defaultLimit))
      .result

  def updateStatus(uid: Uid, newStatus: Status): DBIO[Int] = {
    val query = for {
      l <- LibraryTable.library if l.uid === uid.value
    } yield l.status

    query.update(newStatus.value)
  }

  def delete(uid: Uid): DBIO[Int] = {
    LibraryTable.library
      .filter(_.uid === uid.value)
      .delete
  }
}
