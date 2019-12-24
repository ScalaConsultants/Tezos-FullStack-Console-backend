package io.scalac.tezos.translator.routes.dto

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import io.scalac.tezos.translator.model.LibraryEntry.PendingApproval
import io.scalac.tezos.translator.model.{EmailAddress, LibraryEntry, Uid}
import scala.util.{Success, Try}

sealed trait LibraryEntryDTO

object LibraryEntryDTO {

  implicit val LibraryEntryDTOEncoder: Encoder[LibraryEntryDTO] = {
    case v: LibraryEntryRoutesAdminDto =>
      LibraryEntryRoutesAdminDto.libraryEntryRoutesAdminDtoEncoder(v)
    case v: LibraryEntryRoutesDto =>
      LibraryEntryRoutesDto.libraryEntryRoutesDtoEncoder(v)
  }

}

case class LibraryEntryRoutesAdminDto(
                                       uid: String,
                                       name: String,
                                       author: String,
                                       email: Option[String],
                                       description: String,
                                       micheline: String,
                                       michelson: String,
                                       status: String,
                                     ) extends LibraryEntryDTO

object LibraryEntryRoutesAdminDto {
  def fromDomain(v: LibraryEntry): LibraryEntryRoutesAdminDto = {
    LibraryEntryRoutesAdminDto(
      uid = v.uid.value,
      name = v.name,
      author = v.author,
      email = v.email.map(_.toString),
      description = v.description,
      micheline = v.micheline,
      michelson = v.michelson,
      status = v.status.toString
    )
  }

  implicit val libraryEntryRoutesAdminDtoEncoder: Encoder[LibraryEntryRoutesAdminDto] =
    deriveEncoder[LibraryEntryRoutesAdminDto]

}

case class LibraryEntryRoutesDto(
                                  name: String,
                                  author: String,
                                  email: Option[String],
                                  description: String,
                                  micheline: String,
                                  michelson: String
                                ) extends LibraryEntryDTO {
  def toDomain: Try[LibraryEntry] = {
    val emailAddress = email match {
      case Some(v) => EmailAddress.fromString(v).map(Some(_))
      case None => Success(None)
    }

    emailAddress.map { ea =>
      LibraryEntry(
        uid = Uid(),
        name = name,
        author = author,
        email = ea,
        description = description,
        micheline = micheline,
        michelson = michelson,
        status = PendingApproval
      )
    }
  }
}

object LibraryEntryRoutesDto {
  def fromDomain(v: LibraryEntry): LibraryEntryRoutesDto =
    LibraryEntryRoutesDto(
      name = v.name,
      author = v.author,
      email = v.email.map(_.toString),
      description = v.description,
      micheline = v.micheline,
      michelson = v.michelson
    )

  implicit val libraryEntryRoutesDtoEncoder: Encoder[LibraryEntryRoutesDto] =
    deriveEncoder[LibraryEntryRoutesDto]

}