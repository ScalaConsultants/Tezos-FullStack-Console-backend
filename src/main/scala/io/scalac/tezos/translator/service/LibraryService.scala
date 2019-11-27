package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.model.LibraryEntry
import io.scalac.tezos.translator.model.LibraryEntry._
import io.scalac.tezos.translator.repository.LibraryRepository
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class LibraryService(repository: LibraryRepository, db: Database) {

  def addNew(entry: LibraryEntry): Future[Int] =
    db.run(repository.add(entry.toDbDto))

  def getAccepted(count: Int)(implicit ec: ExecutionContext): Future[Seq[LibraryEntry]] =
    getAll(count, Some(Accepted))

  def getAll(count: Int, statusFilter: Option[Status] = None)(implicit ec: ExecutionContext): Future[Seq[LibraryEntry]] =
    for {
      entriesDto  <-  db.run(repository.list(statusFilter, count))
      entriesFSeq =   entriesDto.map(e => Future.fromTry(LibraryEntry.fromDbDTO(e)))
      entries     <-  Future.sequence(entriesFSeq)
    } yield entries
}
