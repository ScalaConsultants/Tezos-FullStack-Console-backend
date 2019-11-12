package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.model.LibraryDTO
import io.scalac.tezos.translator.repository.LibraryRepository
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

class LibraryService(repository: LibraryRepository, db: Database) {

  def addNew(dto: LibraryDTO): Future[Int] =
    db.run {
      sqlu"""insert into library (name, author, description, micheline, michelson) values
            (${dto.name}, ${dto.author}, ${dto.description}, ${dto.micheline}, ${dto.michelson})"""
    }

}
