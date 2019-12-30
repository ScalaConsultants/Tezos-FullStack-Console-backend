package io.scalac.tezos.translator.model.types

import eu.timepit.refined.{W, refineV}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.{And, Not}
import eu.timepit.refined.collection.{NonEmpty, Size}
import eu.timepit.refined.generic.Equal
import eu.timepit.refined.numeric.Greater
import eu.timepit.refined.string.StartsWith
import io.estatico.newtype.macros.newtype
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.DecodeResult.Value
import sttp.tapir.{Codec, DecodeFailure, DecodeResult, Schema, SchemaType}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import io.circe.syntax._
import cats.syntax.either._
import cats.syntax.option._
import Util._

object Auth {

  type AuthBearerHeaderEntryType = StartsWith[W.`"Bearer "`.T] And Size[Greater[W.`7`.T]]

  type UserTokenType = Size[Equal[W.`30`.T]]

  type UsernameType = NonEmpty And Size[Not[Greater[W.`30`.T]]]

  @newtype case class AuthBearerHeader(v: String Refined AuthBearerHeaderEntryType)

  @newtype case class Password(v: String Refined NonEmpty)

  @newtype case class PasswordHash(v: String Refined NonEmpty)

  @newtype case class UserToken(v: String Refined UserTokenType)

  @newtype case class Username(v: String Refined UsernameType)

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

  implicit val authHeaderStringCodec: Codec[AuthBearerHeader, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(decodeAuthHeader)(encodeToString(_))

  implicit val userTokenStringCodec: Codec[UserToken, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(decodeTokenCodec)(t => s"Bearer ${t.v.value}")

  implicit val usernameEncoder: Encoder[Username] = (a: Username) => a.toString.asJson
  implicit val usernameDecoder: Decoder[Username] = (c: HCursor) => c.as[String] match {
    case Left(_)      => DecodingFailure("Can't username", c.history).asLeft
    case Right(value) => refineV[UsernameType](value) match {
      case Left(refineEr)     => DecodingFailure(refineEr, c.history).asLeft
      case Right(refineValue) => Username(refineValue).asRight
    }
  }

  implicit val passwordEncoder: Encoder[Password] = (a: Password) => a.toString.asJson
  implicit val passwordDecoder: Decoder[Password] = (c: HCursor) => c.as[String] match {
    case Left(_)      => DecodingFailure("Can't parse password", c.history).asLeft
    case Right(value) => refineV[NonEmpty](value) match {
      case Left(refineEr)     => DecodingFailure(refineEr, c.history).asLeft
      case Right(refineValue) => Password(refineValue).asRight
    }
  }

  implicit val usernameSchema: Schema[Username] =
    new Schema[Username](SchemaType.SString, false, "Username".some)

  implicit val passwordSchema: Schema[Password] =
    new Schema[Password](SchemaType.SString, false, "Password of user".some)

  implicit val usernameMapper: JdbcType[Username] with BaseTypedType[Username] =
    refinedMapper2String[Username, UsernameType](Username.apply)

  implicit val passwordHashMapper: JdbcType[PasswordHash] with BaseTypedType[PasswordHash] =
    refinedMapper2String[PasswordHash, NonEmpty](PasswordHash.apply)

}
