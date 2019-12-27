package io.scalac.tezos.translator.model

import java.util.UUID
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.generic.Equal
import eu.timepit.refined.numeric.{Greater, Positive}
import eu.timepit.refined.string._
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype
import io.scalac.tezos.translator.model.Types._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.DecodeResult.Value
import sttp.tapir._
import io.circe.syntax._
import cats.syntax.either._
import cats.syntax.option._
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object Types {

  type AuthBearerHeaderEntryType = StartsWith[W.`"Bearer "`.T] And Size[Greater[W.`7`.T]]

  type UserTokenType = Size[Equal[W.`30`.T]]

  @newtype case class AuthBearerHeader(v: String Refined AuthBearerHeaderEntryType)

  @newtype case class Limit(v: Int Refined Positive)

  @newtype case class LibraryEntryId(v: UUID)

  @newtype case class Offset(v: Int Refined Positive)

  @newtype case class SendEmailId(v: UUID)

  @newtype case class UserToken(v: String Refined UserTokenType)

}

object TypesStuff {

  implicit def coercibleClassTag[R, N](implicit ev: Coercible[ClassTag[R], ClassTag[N]], R: ClassTag[R]): ClassTag[N] =
    ev(R)

  def buildUUIDTypeMapper[T: ClassTag](build: UUID => T): JdbcType[T] with BaseTypedType[T] =
    MappedColumnType.base[T, String](s => s.toString, s => build(UUID.fromString(s)))

  def buildUUIDEncoder[T]: Encoder[T] = (a: T) => a.toString.asJson
  def buildUUIDDecoder[T](build: UUID => T): Decoder[T] = (c: HCursor) => c.as[UUID] match {
    case Left(_)      => DecodingFailure("Can't parse uuid", c.history).asLeft
    case Right(value) => build(value).asRight
  }

  def decodeWithPositiveInt[T](s: String, build: Int Refined Positive => T): DecodeResult[T] = Try(s.toInt) match {
    case Success(v) =>
      refineV[Positive](v) match {
        case Left(_)      => DecodeResult.Mismatch("Positive int", s)
        case Right(value) => DecodeResult.Value(build(value))
      }
    case Failure(f) => DecodeResult.Error(s, f)
  }

  def encodeToString[T](item: T): String = item.toString

  def decodeAuthHeader(s: String): DecodeResult[AuthBearerHeader] =
    refineV[AuthBearerHeaderEntryType](s) match {
      case Left(error)  => DecodeResult.Mismatch(error, s)
      case Right(value) => DecodeResult.Value(AuthBearerHeader(value))
    }

  def decodeTokenCodec(s: String): DecodeResult[UserToken] =
    decodeAuthHeader(s) match {
      case Value(v) => refineV[UserTokenType](v.v.value.drop(7)) match {
        case Left(error)  => DecodeResult.Mismatch(error, s)
        case Right(value) => Value(UserToken(value))
      }
      case failure: DecodeFailure => failure
    }

  implicit val limitStringCodec: Codec[Limit, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(decodeWithPositiveInt[Limit](_, Limit.apply))(encodeToString(_))

  implicit val offsetStringCodec: Codec[Offset, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(decodeWithPositiveInt[Offset](_, Offset.apply))(encodeToString(_))

  implicit val authHeaderStringCodec: Codec[AuthBearerHeader, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(decodeAuthHeader)(encodeToString(_))

  implicit val userTokenStringCodec: Codec[UserToken, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(decodeTokenCodec)(t => s"Bearer ${t.v.value}")

  implicit val libraryEntryId: Schema[LibraryEntryId] =
    new Schema[LibraryEntryId](SchemaType.SString, false, "UUID of library entry".some)
  implicit val libraryEntryIdEncoder: Encoder[LibraryEntryId] = buildUUIDEncoder
  implicit val libraryEntryIdDecoder: Decoder[LibraryEntryId] = buildUUIDDecoder(LibraryEntryId.apply)

  implicit val libraryIdMapper: JdbcType[LibraryEntryId] with BaseTypedType[LibraryEntryId] =
    buildUUIDTypeMapper(LibraryEntryId.apply)

  implicit val sendEmailIdMapper: JdbcType[SendEmailId] with BaseTypedType[SendEmailId] =
    buildUUIDTypeMapper(SendEmailId.apply)

}

object UUIDTypesGenerator {

  def generateUUID[T](build: UUID => T): T = build(UUID.randomUUID())

  def generateLibraryEntryId: LibraryEntryId = generateUUID(LibraryEntryId.apply)

  def generateSendEmailId: SendEmailId = generateUUID(SendEmailId.apply)

}
