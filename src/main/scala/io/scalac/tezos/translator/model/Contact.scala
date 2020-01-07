package io.scalac.tezos.translator.model

import scala.util.{ Failure, Success, Try }
import io.scalac.tezos.translator.model.types.ContactData.Phone
sealed trait Contact extends Product with Serializable

case class ContactPhone(number: Phone) extends Contact
case class ContactEmail(email: EmailAddress) extends Contact
case class FullContact(phone: Phone, email: EmailAddress) extends Contact

object Contact {

  def create(phone: Option[Phone], email: Option[EmailAddress]): Try[Contact] =
    (phone, email) match {
      case (Some(phone), Some(email)) => Success(FullContact(phone, email))
      case (Some(phone), None)        => Success(ContactPhone(phone))
      case (None, Some(email))        => Success(ContactEmail(email))
      case _                          => Failure(new Exception("Empty Email and Empty Phone Number"))
    }

  def prettyString(c: Contact): String =
    c match {
      case ContactPhone(v)   => Seq(s"phone: $v").mkString("") + "\n"
      case ContactEmail(v)   => Seq(s"email: $v").mkString("") + "\n"
      case FullContact(a, b) => Seq(s"phone: $a", s"email: $b").mkString("\n") + "\n"
    }
}
