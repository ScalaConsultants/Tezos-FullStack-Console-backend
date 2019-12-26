package io.scalac.tezos.translator.model

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.generic.Equal
import eu.timepit.refined.numeric.{Greater, Positive}
import eu.timepit.refined.string._
import io.estatico.newtype.macros.newtype
import io.scalac.tezos.translator.model.Types._
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.DecodeResult.Value
import sttp.tapir._
import scala.util.{Failure, Success, Try}

object Types {

  type AuthBearerHeaderEntryType = StartsWith[W.`"Bearer "`.T] And Size[Greater[W.`7`.T]]

  type UserTokenType = Size[Equal[W.`30`.T]]

  @newtype case class AuthBearerHeader(v: String Refined AuthBearerHeaderEntryType)

  @newtype case class Limit(v: Int Refined Positive)

  @newtype case class Offset(v: Int Refined Positive)

  @newtype case class UserToken(v: String Refined UserTokenType)

}

object TypesCodecs {

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

}
