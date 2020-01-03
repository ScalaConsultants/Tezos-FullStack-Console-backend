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
import io.circe.{Decoder, Encoder}
import cats.syntax.option._
import Util._

object Auth {

  type AuthBearerHeaderEntryReq = StartsWith[W.`"Bearer "`.T] And Size[Greater[W.`7`.T]]

  type UserTokenReq = Size[Equal[W.`30`.T]]

  type UsernameReq = NonEmpty And Size[Not[Greater[W.`30`.T]]]

  @newtype case class AuthBearerHeader(v: String Refined AuthBearerHeaderEntryReq)

  @newtype case class Captcha(v: String Refined NonEmpty)

  @newtype case class Password(v: String Refined NonEmpty)

  @newtype case class PasswordHash(v: String Refined NonEmpty)

  @newtype case class UserToken(v: String Refined UserTokenReq)

  @newtype case class Username(v: String Refined UsernameReq)

  def decodeAuthHeader(s: String): DecodeResult[AuthBearerHeader] =
    buildDecoderFromStringWithRefine[AuthBearerHeader, AuthBearerHeaderEntryReq](s, AuthBearerHeader.apply)

  def decodeCaptchaHeader(s: String): DecodeResult[Captcha] =
    buildDecoderFromStringWithRefine[Captcha, NonEmpty](s, Captcha.apply)

  def decodeTokenCodec(s: String): DecodeResult[UserToken] =
    decodeAuthHeader(s) match {
      case Value(v) =>
        refineV[UserTokenReq](v.v.value.drop(7)) match {
          case Left(error)  => DecodeResult.Mismatch(error, s)
          case Right(value) => Value(UserToken(value))
        }
      case failure: DecodeFailure => failure
    }

  implicit val authHeaderStringCodec: Codec[AuthBearerHeader, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(decodeAuthHeader)(encodeToString)

  implicit val captchaHeaderStringCodec: Codec[Captcha, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(decodeCaptchaHeader)(encodeToString)

  implicit val userTokenStringCodec: Codec[UserToken, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(decodeTokenCodec)(t => s"Bearer ${t.v.value}")

  implicit val usernameEncoder: Encoder[Username] = buildToStringEncoder
  implicit val usernameDecoder: Decoder[Username] =
    buildStringRefinedDecoder("Can't parse username", Username.apply)

  implicit val passwordEncoder: Encoder[Password] = buildToStringEncoder
  implicit val passwordDecoder: Decoder[Password] =
    buildStringRefinedDecoder("Can't parse password", Password.apply)

  implicit val usernameSchema: Schema[Username] =
    new Schema[Username](SchemaType.SString, false, "Username".some)

  implicit val passwordSchema: Schema[Password] =
    new Schema[Password](SchemaType.SString, false, "Password of user".some)

  implicit val usernameMapper: JdbcType[Username] with BaseTypedType[Username] =
    refinedMapper2String[Username, UsernameReq](Username.apply)

  implicit val passwordHashMapper: JdbcType[PasswordHash] with BaseTypedType[PasswordHash] =
    refinedMapper2String[PasswordHash, NonEmpty](PasswordHash.apply)

}
