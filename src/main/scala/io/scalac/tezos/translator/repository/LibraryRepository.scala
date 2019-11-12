package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.model.LibraryModel
import io.scalac.tezos.translator.schema.LibraryTable
import io.scalac.tezos.translator.schema._
import slick.jdbc.MySQLProfile.api._

class LibraryRepository {

  val acceptedStatus = 1

  def accepted(max: Int): DBIO[Seq[LibraryModel]] =
    LibraryTable.library
      .filter(_.status === acceptedStatus)
      .sortBy(_.createdAt.desc)
      .take(max)
      .result

}
