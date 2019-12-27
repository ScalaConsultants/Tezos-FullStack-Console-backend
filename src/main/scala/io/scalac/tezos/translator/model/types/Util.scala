package io.scalac.tezos.translator.model.types

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV
import io.estatico.newtype.Coercible
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

  private[types] def encodeToString[T](item: T): String = item.toString

  implicit def coercibleClassTag[R, N](implicit ev: Coercible[ClassTag[R], ClassTag[N]],
                                                      R: ClassTag[R]): ClassTag[N] = ev(R)

}
