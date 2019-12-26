package io.scalac.tezos.translator.service

import akka.event.LoggingAdapter
import io.scalac.tezos.translator.model.LibraryEntry._
import io.scalac.tezos.translator.model.Types.{Limit, Offset}
import io.scalac.tezos.translator.model.{LibraryEntry, Uid}
import io.scalac.tezos.translator.repository.LibraryRepository
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class LibraryService(repository: LibraryRepository, log: LoggingAdapter)(implicit ec: ExecutionContext) {

  def addNew(entry: LibraryEntry): Future[Int] =
    repository.add(LibraryEntryDbDto.fromDomain(entry))

  def getRecords(offset: Option[Offset] = None, limit: Option[Limit] = None, statusFilter: Option[Status] = None): Future[Seq[LibraryEntry]] =
      for {
        entriesDto <- repository.list(statusFilter, offset, limit)
        entriesFSeq = entriesDto.map(e => Future.fromTry(e.toDomain))
        entries <- Future.sequence(entriesFSeq)
      } yield entries

  def changeStatus(uid: Uid, newStatus: Status): Future[LibraryEntry] = {
    val uidNotExistsException = Future.failed(new IllegalArgumentException(s"Library Entry does not exist for uid: ${uid.value}"))

    for {
      entry       <-  repository.get(uid).flatMap {
                        case Some(v) => Future.successful(v)
                        case None => uidNotExistsException
                      }
      updatedDto  <-  repository.update(uid, entry.copy(status = newStatus.value)).flatMap {
                        case Some(v) => Future.successful(v)
                        case None => uidNotExistsException
                      }
      updated     <-  updatedDto.toDomain match {
                        case Success(e) => Future.successful(e)
                        case Failure(ex) =>
                          log.error(s"Invalid library entry data in DB for uid: ${uid.value} ! ${ex.getMessage}")
                          Future.failed(new IllegalStateException(s"Library entry for ${uid.value} has a wrong format in DB ! ${ex.getMessage}"))
                      }
    } yield updated
  }

  def delete(uid: Uid): Future[Unit] =
    repository.delete(uid).flatMap {
      case 0 => Future.failed(new IllegalArgumentException(s"There is no Library Entry for uid: $uid"))
      case 1 => Future.successful(())
    }
}
