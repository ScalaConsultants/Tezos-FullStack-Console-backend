package io.scalac.tezos.translator.model.types

import java.util.UUID
import io.estatico.newtype.macros.newtype
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import sttp.tapir.{Schema, SchemaType}
import io.circe.syntax._
import cats.syntax.either._
import cats.syntax.option._
import scala.reflect.ClassTag
import Util._

object UUIDs {

  @newtype case class LibraryEntryId(v: UUID)

  @newtype case class SendEmailId(v: UUID)

  def buildUUIDTypeMapper[T: ClassTag](build: UUID => T): JdbcType[T] with BaseTypedType[T] =
    MappedColumnType.base[T, String](s => s.toString, s => build(UUID.fromString(s)))

  def buildUUIDEncoder[T]: Encoder[T] = (a: T) => a.toString.asJson
  def buildUUIDDecoder[T](build: UUID => T): Decoder[T] = (c: HCursor) => c.as[UUID] match {
    case Left(_)      => DecodingFailure("Can't parse uuid", c.history).asLeft
    case Right(value) => build(value).asRight
  }

  def generateUUID[T](build: UUID => T): T = build(UUID.randomUUID())

  def generateLibraryEntryId: LibraryEntryId = generateUUID(LibraryEntryId.apply)

  def generateSendEmailId: SendEmailId = generateUUID(SendEmailId.apply)

  implicit val libraryEntryId: Schema[LibraryEntryId] =
    new Schema[LibraryEntryId](SchemaType.SString, false, "UUID of library entry".some)
  implicit val libraryEntryIdEncoder: Encoder[LibraryEntryId] = buildUUIDEncoder
  implicit val libraryEntryIdDecoder: Decoder[LibraryEntryId] = buildUUIDDecoder(LibraryEntryId.apply)

  implicit val libraryIdMapper: JdbcType[LibraryEntryId] with BaseTypedType[LibraryEntryId] =
    buildUUIDTypeMapper(LibraryEntryId.apply)

  implicit val sendEmailIdMapper: JdbcType[SendEmailId] with BaseTypedType[SendEmailId] =
    buildUUIDTypeMapper(SendEmailId.apply)

}
