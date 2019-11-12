package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.model.LibraryDTO
import io.scalac.tezos.translator.repository.LibraryRepository
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class LibraryService(repository: LibraryRepository, db: Database) {

  def addNew(dto: LibraryDTO): Future[Int] =
    db.run {
      sqlu"""insert into library (name, author, description, micheline, michelson) values
            (${dto.name}, ${dto.author}, ${dto.description}, ${dto.micheline}, ${dto.michelson})"""
    }

  def getAcceptedAsDto(count: Int)(implicit ec: ExecutionContext): Future[Seq[LibraryDTO]] =
    for {
      models <- db.run(repository.accepted(count))
    } yield models
      .map(
        model =>
          LibraryDTO(
            name        = model.name,
            author      = model.author,
            description = model.description,
            micheline   = model.micheline,
            michelson   = model.michelson
          )
      )

}
