package io.scalac.tezos.translator.model.types

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV
import io.estatico.newtype.Coercible
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import sttp.tapir.DecodeResult
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object Util {

  private[types] def decodeWithPositiveInt[T](s: String, build: Int Refined Positive => T): DecodeResult[T] = Try(s.toInt) match {
    case Success(v) =>
      refineV[Positive](v) match {
        case Left(_)      => DecodeResult.Mismatch("Positive int", s)
        case Right(value) => DecodeResult.Value(build(value))
      }
    case Failure(f) => DecodeResult.Error(s, f)
  }

  private[types] def refinedMapper2String[T: ClassTag, Req](build: Refined[String, Req] => T)
                                                           (implicit v: Validate[String, Req]): JdbcType[T] with BaseTypedType[T] =
    MappedColumnType.base[T, String](encodeToString, s => build(refineV[Req](s).toOption.get))

  private[types] def encodeToString[T](item: T): String = item.toString

  private[types] implicit def coercibleClassTag[R, N](implicit ev: Coercible[ClassTag[R], ClassTag[N]],
                                                      R: ClassTag[R]): ClassTag[N] = ev(R)

}
