package io.scalac.tezos.translator.model

import io.circe.{Decoder, Encoder}

import scala.util.{Failure, Success, Try}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
sealed trait Contact extends Product with Serializable

case class ContactPhone(number: String) extends Contact
case class ContactEmail(email: EmailAddress) extends Contact
case class FullContact(phone: String, email: EmailAddress) extends Contact

object Contact {

  implicit val FullContactDecoder: Decoder[FullContact] = Decoder.forProduct2("phone","email")(FullContact.apply)
  implicit val FullContactEncoder: Encoder[FullContact] = Encoder.forProduct2("phone","email")(u=> (u.phone,u.email))
  implicit val ContactEmailDecoder: Decoder[ContactEmail] = Decoder.forProduct1("email")(ContactEmail.apply)
  implicit val ContactEmailEncoder: Encoder[ContactEmail] = Encoder.forProduct1("email")(u=> (u.email))
  implicit val ContactPhoneDecoder: Decoder[ContactPhone] = Decoder.forProduct1("number")(ContactPhone.apply)
  implicit val ContactPhoneEncoder: Encoder[ContactPhone] = Encoder.forProduct1("number")(u=> (u.number))

  def tryToCreateContact(phone: Option[String], email: Option[EmailAddress]): Try[Contact] =
    (phone, email) match {
      case (Some(phone), Some(email)) => Success(FullContact(phone, email))
      case (Some(phone), None)  => Success(ContactPhone(phone))
      case (_, Some(email)) => Success(ContactEmail(email))
      case _ => Failure( new Exception("Empty Email and Empty Phone Number"))
    }
  def getValuesFromContact(c: Contact): String =
  c match {
        case ContactPhone(v) => Seq(s"phone: $v").mkString("")
        case ContactEmail(v) => Seq(s"email: $v").mkString("")
        case FullContact(a, b) => Seq(s"phone: $a", s"email: $b").mkString("\n")+"\n"
      }
}

