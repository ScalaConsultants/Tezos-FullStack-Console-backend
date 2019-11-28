package io.scalac.tezos.translator.model

import io.scalac.tezos.translator.model.LibraryEntry.Status

import scala.util.{Failure, Success, Try}

case class LibraryEntry(
  uid: Uid,
  name: String,
  author: String,
  email: Option[String],
  description: String,
  micheline: String,
  michelson: String,
  status: Status
)

object LibraryEntry {

  sealed trait Status extends Product with Serializable {
    def value: Int = this match {
      case PendingApproval => 0
      case Accepted => 1
      case Declined => 2
    }
  }

  object Status {
    def fromInt(i: Int): Try[Status] = i match {
      case 0 => Success(PendingApproval)
      case 1 => Success(Accepted)
      case 2 => Success(Declined)
      case _ => Failure(new IllegalArgumentException(s"Given Int is not a valid Status. Got: $i"))
    }
  }

  case object PendingApproval extends Status
  case object Accepted extends Status
  case object Declined extends Status

}
