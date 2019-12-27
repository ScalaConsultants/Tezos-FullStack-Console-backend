package io.scalac.tezos.translator.routes.dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.generic.semiauto.deriveDecoder
import io.scalac.tezos.translator.model.LibraryEntry.PendingApproval
import io.scalac.tezos.translator.model.{EmailAddress, LibraryEntry, Uid}
import scala.util.{Success, Try}

sealed trait LibraryEntryDTO

object LibraryEntryDTO {

  implicit val LibraryEntryDTOEncoder: Encoder[LibraryEntryDTO] = {
    case v: LibraryEntryRoutesAdminDto =>
      LibraryEntryRoutesAdminDto.libraryEntryRoutesAdminDtoEncoder(v)
    case v: LibraryEntryRoutesDto      =>
      LibraryEntryRoutesDto.libraryEntryRoutesDtoEncoder(v)
  }

}

case class LibraryEntryRoutesAdminDto(
                                       uid: String,
                                       title: String,
                                       author: Option[String],
                                       email: Option[String],
                                       description: Option[String],
                                       micheline: String,
                                       michelson: String,
                                       status: String,
                                     ) extends LibraryEntryDTO

object LibraryEntryRoutesAdminDto {
  def fromDomain(v: LibraryEntry): LibraryEntryRoutesAdminDto = {
    LibraryEntryRoutesAdminDto(
      uid = v.uid.value,
      title = v.title,
      author = v.author.map(_.toString),
      email = v.email.map(_.toString),
      description = v.description.map(_.toString),
      micheline = v.micheline,
      michelson = v.michelson,
      status = v.status.toString
    )
  }

  implicit val libraryEntryRoutesAdminDtoEncoder: Encoder[LibraryEntryRoutesAdminDto] =
    deriveEncoder[LibraryEntryRoutesAdminDto]

  implicit val libraryEntryRoutesAdminDtoDecoder: Decoder[LibraryEntryRoutesAdminDto] =
    deriveDecoder[LibraryEntryRoutesAdminDto]

}

case class LibraryEntryRoutesDto(
                                  title: String,
                                  author: Option[String],
                                  email: Option[String],
                                  description: Option[String],
                                  micheline: String,
                                  michelson: String
                                ) extends LibraryEntryDTO {
  def toDomain: Try[LibraryEntry] = {

    val emailAdress = email match {
      case Some(e) => EmailAddress.fromString(e).map(Some(_))
      case None => Success(None)
    }

    emailAdress.map(email => {

      LibraryEntry(
        uid = Uid(),
        title = title,
        author = author,
        email = email,
        description = description,
        micheline = micheline,
        michelson = michelson,
        status = PendingApproval
      )
    })
  }

}
object LibraryEntryRoutesDto {
  def fromDomain(v: LibraryEntry): LibraryEntryRoutesDto =
    LibraryEntryRoutesDto(
      title = v.title,
      author = v.author,
      email = v.email.map(_.toString),
      description = v.description,
      micheline = v.micheline,
      michelson = v.michelson
    )

  implicit val libraryEntryRoutesDtoEncoder: Encoder[LibraryEntryRoutesDto] =
    deriveEncoder[LibraryEntryRoutesDto]

  implicit val libraryEntryRoutesDtoDecoder: Decoder[LibraryEntryRoutesDto] =
    deriveDecoder[LibraryEntryRoutesDto]

}
