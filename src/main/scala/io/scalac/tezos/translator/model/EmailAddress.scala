package io.scalac.tezos.translator.model


sealed trait EmailAddress extends Product with Serializable

case object AdminEmail extends EmailAddress

case class UserEmail(v: String) extends EmailAddress

object EmailAddress {
  def toString(a: EmailAddress): String = a match {
    case AdminEmail => "admin"
    case UserEmail(v) => v
  }

}