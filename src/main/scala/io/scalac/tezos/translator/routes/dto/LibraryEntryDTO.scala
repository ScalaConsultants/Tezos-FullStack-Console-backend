package io.scalac.tezos.translator.routes.dto

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }
import io.scalac.tezos.translator.model.LibraryEntry.PendingApproval
import io.scalac.tezos.translator.model.types.ContactData.{ EmailS, MaybeEmailAddressOps }
import io.scalac.tezos.translator.model.types.Library._
import io.scalac.tezos.translator.model.types.UUIDs._
import io.scalac.tezos.translator.model.{ EmailAddress, LibraryEntry }

import scala.util.{ Success, Try }

sealed trait LibraryEntryDTO

object LibraryEntryDTO {

  implicit val LibraryEntryDTOEncoder: Encoder[LibraryEntryDTO] = {
    case v: LibraryEntryRoutesAdminDto => LibraryEntryRoutesAdminDto.libraryEntryRoutesAdminDtoEncoder(v)
    case v: LibraryEntryRoutesNewDto   => LibraryEntryRoutesNewDto.libraryEntryRoutesNewDtoEncoder(v)
    case v: LibraryEntryRoutesDto      => LibraryEntryRoutesDto.libraryEntryRoutesDtoEncoder(v)
  }

}

/** LibraryEntryRoutesAdminDto **/

case class LibraryEntryRoutesAdminDto(
   uid: LibraryEntryId,
   title: Title,
   author: Option[Author],
   email: Option[EmailS],
   description: Option[Description],
   micheline: Micheline,
   michelson: Michelson,
   status: String)
    extends LibraryEntryDTO

object LibraryEntryRoutesAdminDto {

  def fromDomain(v: LibraryEntry): LibraryEntryRoutesAdminDto =
    LibraryEntryRoutesAdminDto(
       uid         = v.uid,
       title       = v.title,
       author      = v.author,
       email       = v.email.toEmailS,
       description = v.description,
       micheline   = v.micheline,
       michelson   = v.michelson,
       status      = v.status.toString
    )

  implicit val libraryEntryRoutesAdminDtoEncoder: Encoder[LibraryEntryRoutesAdminDto] =
    deriveEncoder[LibraryEntryRoutesAdminDto]

  implicit val libraryEntryRoutesAdminDtoDecoder: Decoder[LibraryEntryRoutesAdminDto] =
    deriveDecoder[LibraryEntryRoutesAdminDto]

}

/** LibraryEntryRoutesNewDto **/

case class LibraryEntryRoutesNewDto(
   title: Title,
   author: Option[Author],
   email: Option[EmailS],
   description: Option[Description],
   micheline: Micheline,
   michelson: Michelson)
    extends LibraryEntryDTO {

  def toDomain: Try[LibraryEntry] = {

    val emailAdress = email match {
      case Some(e) => EmailAddress.fromString(e.v.value).map(Some(_))
      case None    => Success(None)
    }

    emailAdress.map { email =>
      LibraryEntry(
         uid         = generateLibraryEntryId,
         title       = title,
         author      = author,
         email       = email,
         description = description,
         micheline   = micheline,
         michelson   = michelson,
         status      = PendingApproval
      )
    }
  }

}

object LibraryEntryRoutesNewDto {

  implicit val libraryEntryRoutesNewDtoEncoder: Encoder[LibraryEntryRoutesNewDto] =
    deriveEncoder[LibraryEntryRoutesNewDto]

  implicit val libraryEntryRoutesNewDtoDecoder: Decoder[LibraryEntryRoutesNewDto] =
    deriveDecoder[LibraryEntryRoutesNewDto]

}

/** LibraryEntryRoutesDto **/

case class LibraryEntryRoutesDto(
   uid: LibraryEntryId,
   title: Title,
   author: Option[Author],
   email: Option[EmailS],
   description: Option[Description],
   micheline: Micheline,
   michelson: Michelson)
    extends LibraryEntryDTO

object LibraryEntryRoutesDto {

  def fromDomain(v: LibraryEntry): LibraryEntryRoutesDto =
    LibraryEntryRoutesDto(
       uid         = v.uid,
       title       = v.title,
       author      = v.author,
       email       = v.email.toEmailS,
       description = v.description,
       micheline   = v.micheline,
       michelson   = v.michelson
    )

  implicit val libraryEntryRoutesDtoEncoder: Encoder[LibraryEntryRoutesDto] =
    deriveEncoder[LibraryEntryRoutesDto]

  implicit val libraryEntryRoutesDtoDecoder: Decoder[LibraryEntryRoutesDto] =
    deriveDecoder[LibraryEntryRoutesDto]

}
