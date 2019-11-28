package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.model.LibraryEntry.Status
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
}
