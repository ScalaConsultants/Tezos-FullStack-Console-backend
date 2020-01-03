package io.scalac.tezos.translator.model

import io.scalac.tezos.translator.model.LibraryEntry.Status

import scala.util.{Failure, Success, Try}

case class LibraryEntry(
   uid: Uid,
   title: String,
   author: Option[String],
   email: Option[EmailAddress],
   description: Option[String],
   micheline: String,
   michelson: String,
   status: Status)

object LibraryEntry {

  sealed trait Status extends Product with Serializable {

    def value: Int = this match {
      case PendingApproval => 0
      case Accepted        => 1
      case Declined        => 2
    }

    override def toString: String = this match {
      case PendingApproval => "pending_approval"
      case Accepted        => "accepted"
      case Declined        => "declined"
    }
  }

  object Status {

    def fromInt(i: Int): Try[Status] = i match {
      case 0 => Success(PendingApproval)
      case 1 => Success(Accepted)
      case 2 => Success(Declined)
      case _ => Failure(new IllegalArgumentException(s"Given Int is not a valid Status. Got: $i"))
    }

    def fromString(s: String): Try[Status] = s match {
      case "pending_approval" => Success(PendingApproval)
      case "accepted"         => Success(Accepted)
      case "declined"         => Success(Declined)
      case _                  => Failure(new IllegalArgumentException(s"Given String is not a valid Status. Got: $s"))
    }
  }

  case object PendingApproval extends Status
  case object Accepted extends Status
  case object Declined extends Status

}
