package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.model.LibraryEntry.Status
import io.scalac.tezos.translator.model.Uid
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto
import io.scalac.tezos.translator.schema.LibraryTable
import slick.jdbc.MySQLProfile.api._

class LibraryRepository {

  def add(translation: LibraryEntryDbDto): DBIO[Int] =
    LibraryTable.library += translation

  def list(status: Option[Status], limit: Int): DBIO[Seq[LibraryEntryDbDto]] =
    LibraryTable.library.filterOpt(status){ case (row, s) =>  row.status === s.value}
      .sortBy(_.createdAt.desc)
      .take(limit)
      .result

  def updateStatus(uid: Uid, newStatus: Status): DBIO[Int] = {
    val q = for {
      l <- LibraryTable.library if l.uid === uid.value
    } yield l.status

    q.update(newStatus.value)
  }

  def delete(uid: Uid): DBIO[Int] = {
    LibraryTable.library
      .filter(_.uid === uid.value)
      .delete
  }
}
