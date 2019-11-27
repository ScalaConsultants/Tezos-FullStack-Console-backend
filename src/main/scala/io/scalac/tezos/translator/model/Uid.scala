package io.scalac.tezos.translator.model

import java.util.UUID

import scala.util.{Failure, Success, Try}

sealed abstract case class Uid(value: String)

object Uid {
  def apply(): Uid = {
    val uuid = UUID.randomUUID().toString.toLowerCase.split("-")(0)
    new Uid(uuid) {}
  }

  private val uidRegex: String = """^[0-9a-f]{8}$"""

  def fromString(s: String): Try[Uid] =
    s.trim match {
      case null => failure("Uid cannot be null")
      case "" =>  failure("Uid cannot be an empty String")
      case v =>
        if (v.matches(uidRegex)) Success(new Uid(s) {})
        else failure(s"Given String is not a valid Uid. Got: $s")
    }

  private def failure(msg: String) =
    Failure(new IllegalArgumentException(msg))

}