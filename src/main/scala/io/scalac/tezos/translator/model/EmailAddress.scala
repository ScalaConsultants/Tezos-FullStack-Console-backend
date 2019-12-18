package io.scalac.tezos.translator.model

import io.circe.{Decoder, Encoder}
import javax.mail.internet.InternetAddress

import scala.util.{Success, Try}

sealed abstract case class EmailAddress(value: InternetAddress) {
  override def toString: String = value.getAddress
}

object EmailAddress {
  implicit val emailAddressDecoder: Decoder[EmailAddress] = Decoder.forProduct1("value")(u=>EmailAddress.fromString(u).get) // abstract doesn't have apply so idk what next
  implicit val emailAddressEncoder: Encoder[EmailAddress] = Encoder.forProduct1("value")(u=> (u.value))
  def fromString(s: String): Try[EmailAddress] =
    Try(new InternetAddress(s, true))
      .map(ia => new EmailAddress(ia) {})


}
