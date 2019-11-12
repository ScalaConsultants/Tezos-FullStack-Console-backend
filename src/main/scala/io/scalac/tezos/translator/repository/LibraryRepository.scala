package io.scalac.tezos.translator.repository

import io.scalac.tezos.translator.model.LibraryModel
import io.scalac.tezos.translator.schema.LibraryTable
import io.scalac.tezos.translator.schema._
import slick.jdbc.MySQLProfile.api._
import LibraryRepository._

class LibraryRepository {


  def accepted(max: Int): DBIO[Seq[LibraryModel]] =
    LibraryTable.library
      .filter(_.status === Status.accepted)
      .sortBy(_.createdAt.desc)
      .take(max)
      .result

}

object LibraryRepository {

  object Status {
    val accepted = 1
  }

}
