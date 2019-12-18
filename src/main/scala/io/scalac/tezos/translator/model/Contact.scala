package io.scalac.tezos.translator.model

import scala.util.Try

sealed trait Contact extends Product with Serializable

case class ContactPhone(number: String) extends Contact
case class ContactEmail(email: EmailAddress) extends Contact
case class FullContact(phone: String, email: EmailAddress) extends Contact

object Contact {
  def tryToCreateContact(phone: String, email: EmailAddress): Try[Contact] =
    (phone, email) match {
      case (phone, email) if !email.toString.isEmpty && !phone.isEmpty => Try(FullContact(phone, email))
      case (phone, _) if !phone.isEmpty() => Try(ContactPhone(phone))
      case (_, email) if !email.toString.isEmpty => Try(ContactEmail(email))
      case _ => throw new Exception("Empty Email and Empty Phone Number")
    }
}
