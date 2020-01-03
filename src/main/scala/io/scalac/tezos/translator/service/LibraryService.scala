package io.scalac.tezos.translator.service

import akka.event.LoggingAdapter
import io.scalac.tezos.translator.model.LibraryEntry._
import io.scalac.tezos.translator.model.LibraryEntry
import io.scalac.tezos.translator.model.types.Params.{ Limit, Offset }
import io.scalac.tezos.translator.model.types.UUIDs.LibraryEntryId
import io.scalac.tezos.translator.repository.LibraryRepository
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class LibraryService(repository: LibraryRepository, log: LoggingAdapter)(implicit ec: ExecutionContext) {

  def addNew(entry: LibraryEntry): Future[Int] =
    repository.add(LibraryEntryDbDto.fromDomain(entry))

  def getRecords(
     offset: Option[Offset]       = None,
     limit: Option[Limit]         = None,
     statusFilter: Option[Status] = None
   ): Future[Seq[LibraryEntry]] =
    for {
      entriesDto  <- repository.list(statusFilter, offset, limit)
      entriesFSeq = entriesDto.map(e => Future.fromTry(e.toDomain))
      entries     <- Future.sequence(entriesFSeq)
    } yield entries

  def changeStatus(id: LibraryEntryId, newStatus: Status): Future[LibraryEntry] = {
    val uidNotExistsException =
      Future.failed(new IllegalArgumentException(s"Library Entry does not exist for uid: $id"))

    for {
      entry <- repository.get(id).flatMap {
                case Some(v) => Future.successful(v)
                case None    => uidNotExistsException
              }
      updatedDto <- repository.update(id, entry.copy(status = newStatus.value)).flatMap {
                     case Some(v) => Future.successful(v)
                     case None    => uidNotExistsException
                   }
      updated <- updatedDto.toDomain match {
                  case Success(e) => Future.successful(e)
                  case Failure(ex) =>
                    log.error(s"Invalid library entry data in DB for uid: id ! ${ex.getMessage}")
                    Future.failed(new IllegalStateException(s"Library entry for id has a wrong format in DB ! ${ex.getMessage}"))
                }
    } yield updated
  }

  def delete(id: LibraryEntryId): Future[Unit] =
    repository.delete(id).flatMap {
      case 0 => Future.failed(new IllegalArgumentException(s"There is no Library Entry for uid: $id"))
      case 1 => Future.successful(())
    }
}
