package io.scalac.tezos.translator.model.types

import eu.timepit.refined.{W, refineV}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.generic.Equal
import eu.timepit.refined.numeric.Greater
import eu.timepit.refined.string.StartsWith
import io.estatico.newtype.macros.newtype
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.DecodeResult.Value
import sttp.tapir.{Codec, DecodeFailure, DecodeResult}
import Util.encodeToString

object Auth {

  type AuthBearerHeaderEntryType = StartsWith[W.`"Bearer "`.T] And Size[Greater[W.`7`.T]]

  type UserTokenType = Size[Equal[W.`30`.T]]

  @newtype case class AuthBearerHeader(v: String Refined AuthBearerHeaderEntryType)

  @newtype case class UserToken(v: String Refined UserTokenType)

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

}
