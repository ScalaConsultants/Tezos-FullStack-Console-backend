package io.scalac.tezos.translator.service

import akka.event.LoggingAdapter
import io.scalac.tezos.translator.model.LibraryEntry
import io.scalac.tezos.translator.model.LibraryEntry._
import io.scalac.tezos.translator.model.types.Params.{ Limit, Offset }
import io.scalac.tezos.translator.model.types.UUIDs.LibraryEntryId
import io.scalac.tezos.translator.repository.LibraryRepository
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class LibraryService(repository: LibraryRepository, log: LoggingAdapter)(implicit ec: ExecutionContext) {

  def addNew(entry: LibraryEntry): Future[Int] =
    repository.add(LibraryEntryDbDto.fromDomain(entry))

  def getEntry(id: LibraryEntryId): Future[LibraryEntry] =
    for {
      entryDb <- getEntryDb(id)
      entry   <- entryDbToDomain(entryDb)
    } yield entry

  def getEntryWithOptionalFilter(id: LibraryEntryId, statusFilter: Option[Status] = None): Future[LibraryEntry] =
    resolveAction {
      for {
        entry <- getEntry(id)
      } yield {
        statusFilter match {
          case Some(s) => Some(entry).filter(_.status == s)
          case None    => Some(entry)
        }
      }
    }

  def getEntries(
     offset: Option[Offset]       = None,
     limit: Option[Limit]         = None,
     statusFilter: Option[Status] = None
   ): Future[Seq[LibraryEntry]] =
    for {
      entriesDto <- repository.list(statusFilter, offset, limit)
      entries    <- Future.sequence(entriesDto.map(e => Future.fromTry(e.toDomain)))
    } yield entries

  def changeStatus(id: LibraryEntryId, newStatus: Status): Future[LibraryEntry] =
    for {
      entryDb    <- getEntryDb(id)
      updatedDto <- resolveAction(repository.update(id, entryDb.copy(status = newStatus.value)))
      updated    <- entryDbToDomain(updatedDto)
    } yield updated

  def delete(id: LibraryEntryId): Future[Unit] =
    repository.delete(id).flatMap {
      case 0 => EntryNotFound
      case 1 => Future.successful(())
    }

  private val EntryNotFound =
    Future.failed(new IllegalArgumentException(s"There is no Library Entry with this UUID!"))

  private def resolveAction[T](action: Future[Option[T]]) =
    action.flatMap {
      case Some(entry) => Future.successful(entry)
      case None        => EntryNotFound
    }

  private def getEntryDb(id: LibraryEntryId): Future[LibraryEntryDbDto] =
    resolveAction(repository.get(id))

  private def entryDbToDomain(entryDb: LibraryEntryDbDto) =
    entryDb.toDomain match {
      case Success(e) => Future.successful(e)
      case Failure(ex) =>
        log.error(s"Invalid library entry data in DB for uid: `id`! ${ex.getMessage}")
        Future.failed(new IllegalStateException(s"Library entry for id has a wrong format in DB! ${ex.getMessage}"))
    }

}
