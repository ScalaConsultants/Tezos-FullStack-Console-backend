package io.scalac.tezos.translator.model

import java.sql.Timestamp
import java.time.Instant

import io.scalac.tezos.translator.model.LibraryEntry.Status

import scala.util.{Failure, Success, Try}

case class LibraryEntry(
  uid: Uid,
  name: String,
  author: String,
  email: Option[String],
  description: String,
  micheline: String,
  michelson: String,
  status: Status
) {
  def toJsonDTO: LibraryJsonDTO =
    LibraryJsonDTO(
      name = name,
      author = author,
      email = email,
      description = description,
      micheline = micheline,
      michelson = michelson
    )

  def toDbDto: LibraryDbDTO =
    LibraryDbDTO(
      id = None,
      uid = uid.value,
      name = name,
      author = author,
      email = email,
      description = description,
      micheline = micheline,
      michelson = michelson,
      createdAt = Timestamp.from(Instant.now),
      status = status.value
    )
}

object LibraryEntry {

  def fromJsonDTO(dto: LibraryJsonDTO): LibraryEntry = LibraryEntry(
    uid = Uid(),
    name = dto.name,
    author = dto.author,
    email = dto.email,
    description = dto.description,
    micheline = dto.micheline,
    michelson = dto.michelson,
    status = PendingApproval
  )

  def fromDbDTO(dto: LibraryDbDTO): Try[LibraryEntry] =
    for {
      status <- Status.fromInt(dto.status)
      uid <- Uid.fromString(dto.uid)
    } yield LibraryEntry(
      uid = uid,
      name = dto.name,
      author = dto.author,
      email = dto.email,
      description = dto.description,
      micheline = dto.micheline,
      michelson = dto.michelson,
      status = status
    )

  sealed trait Status extends Product with Serializable {
    def value: Int = this match {
      case PendingApproval => 0
      case Accepted => 1
      case Declined => 2
    }
  }

  object Status {
    def fromInt(i: Int): Try[Status] = i match {
      case 0 => Success(PendingApproval)
      case 1 => Success(Accepted)
      case 2 => Success(Declined)
      case _ => Failure(new IllegalArgumentException(s"Given Int is not a valid Status. Got: $i"))
    }
  }

  case object PendingApproval extends Status
  case object Accepted extends Status
  case object Declined extends Status

}
