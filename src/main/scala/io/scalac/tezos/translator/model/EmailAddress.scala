package io.scalac.tezos.translator.model

import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor }
import javax.mail.internet.InternetAddress

import scala.util.Try

sealed abstract case class EmailAddress(value: InternetAddress) {
  override def toString: String = value.getAddress
}

object EmailAddress {

  def fromString(s: String): Try[EmailAddress] =
    Try(new InternetAddress(s, true))
      .map(ia => new EmailAddress(ia) {})

  implicit val emailAddressEncoder: Encoder[EmailAddress] = Encoder.forProduct1("value")(_.toString)

  implicit val emailAddressDecoder: Decoder[EmailAddress] = new Decoder[EmailAddress] {

    final def apply(c: HCursor): Decoder.Result[EmailAddress] =
      for {
        value <- c.downField("value").as[String]
        email <- EmailAddress
                  .fromString(value)
                  .toEither
                  .left
                  .map(ex => DecodingFailure.fromThrowable(ex, c.history))
      } yield email
  }
}
