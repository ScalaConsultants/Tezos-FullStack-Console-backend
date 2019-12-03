package io.scalac.tezos.translator.service

import io.scalac.tezos.translator.model.{LibraryEntry, Uid}
import io.scalac.tezos.translator.model.LibraryEntry._
import io.scalac.tezos.translator.repository.LibraryRepository
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto
import slick.jdbc.PostgresProfile.api._
import io.scalac.tezos.translator.service.LibraryService.UidNotExists

import scala.concurrent.{ExecutionContext, Future}

class LibraryService(repository: LibraryRepository, db: Database)(implicit ec: ExecutionContext) {

  def addNew(entry: LibraryEntry): Future[Int] =
    db.run(repository.add(LibraryEntryDbDto.fromDomain(entry)))

  def getAccepted(count: Int): Future[Seq[LibraryEntry]] =
    getAll(count, Some(Accepted))

  def getAll(count: Int, statusFilter: Option[Status] = None)(implicit ec: ExecutionContext): Future[Seq[LibraryEntry]] =
    for {
      entriesDto  <-  db.run(repository.list(statusFilter, count))
      entriesFSeq =   entriesDto.map(e => Future.fromTry(e.toDomain))
      entries     <-  Future.sequence(entriesFSeq)
    } yield entries

  def changeStatus(uid: Uid, newStatus: Status): Future[Unit] =
    db.run(repository.updateStatus(uid, newStatus)) flatMap {
      case 0 => Future.failed(UidNotExists(s"There is no Library Entry for uid: $uid"))
      case 1 => Future.successful(())
    }

  def delete(uid: Uid): Future[Unit] =
    db.run(repository.delete(uid)).flatMap {
      case 0 => Future.failed(UidNotExists(s"There is no Library Entry for uid: $uid"))
      case 1 => Future.successful(())
    }

}

object LibraryService {
  sealed trait LibraryServiceError extends Throwable
  case class UidNotExists(msg: String) extends LibraryServiceError
}
