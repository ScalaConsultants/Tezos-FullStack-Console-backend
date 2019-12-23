package io.scalac.tezos.translator.model

import java.util.UUID

import scala.util.{Failure, Success, Try}

sealed abstract case class Uid(value: String)

object Uid {

  def apply(): Uid =
    new Uid(UUID.randomUUID().toString.toLowerCase) {}

  def fromString(s: String): Try[Uid] =
    if (s == null) failure("Uid cannot be null")
    else if (s.trim.isEmpty) failure("Uid cannot be empty")
    else if (isNotUUID(s)) failure(s"Given String is not a valid Uid. Got: $s")
    else Success(new Uid(s) {})

  private def isNotUUID(input: String): Boolean =
    Try(UUID.fromString(input.trim)).isFailure

  private def failure(msg: String) =
    Failure(new IllegalArgumentException(msg))

}
