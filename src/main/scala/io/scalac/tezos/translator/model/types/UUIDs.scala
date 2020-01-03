package io.scalac.tezos.translator.model.types

import java.util.UUID
import io.estatico.newtype.macros.newtype
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import sttp.tapir.{Codec, DecodeResult, Schema, SchemaType}
import cats.syntax.either._
import cats.syntax.option._
import scala.reflect.ClassTag
import Util._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import eu.timepit.refined.refineV
import io.circe.refined._
import sttp.tapir.CodecFormat.TextPlain

object UUIDs {

  type UUIDString = String Refined Uuid

  @newtype case class LibraryEntryId(v: UUIDString)

  @newtype case class SendEmailId(v: UUIDString)

  def buildUUIDTypeMapper[T: ClassTag](build: UUIDString => T): JdbcType[T] with BaseTypedType[T] =
    MappedColumnType.base[T, String](s => s.toString, s => build(refineV[Uuid](s).toOption.get))

  def buildUUIDDecoder[T](build: UUIDString => T): Decoder[T] =
    (c: HCursor) =>
      c.as[String Refined Uuid] match {
        case Left(_)      => DecodingFailure("Can't parse uuid", c.history).asLeft
        case Right(value) => build(value).asRight
      }

  @scala.annotation.tailrec
  def generateUUID[T](build: UUIDString => T): T = refineV[Uuid](UUID.randomUUID().toString) match {
    case Left(_)      => generateUUID(build)
    case Right(value) => build(value)
  }

  def generateLibraryEntryId: LibraryEntryId = generateUUID(LibraryEntryId.apply)

  def generateSendEmailId: SendEmailId = generateUUID(SendEmailId.apply)

  def decodeUuid(s: String): DecodeResult[UUIDString] = buildDecoderFromStringWithRefine[UUIDString, Uuid](s, identity)

  implicit val libraryEntryId: Schema[LibraryEntryId] =
    new Schema[LibraryEntryId](SchemaType.SString, false, "UUID of library entry".some)
  implicit val libraryEntryIdEncoder: Encoder[LibraryEntryId] = buildToStringEncoder
  implicit val libraryEntryIdDecoder: Decoder[LibraryEntryId] = buildUUIDDecoder(LibraryEntryId.apply)

  implicit val libraryIdMapper: JdbcType[LibraryEntryId] with BaseTypedType[LibraryEntryId] =
    buildUUIDTypeMapper(LibraryEntryId.apply)

  implicit val sendEmailIdMapper: JdbcType[SendEmailId] with BaseTypedType[SendEmailId] =
    buildUUIDTypeMapper(SendEmailId.apply)

  implicit val authHeaderStringCodec: Codec[UUIDString, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(decodeUuid)(encodeToString)

}
