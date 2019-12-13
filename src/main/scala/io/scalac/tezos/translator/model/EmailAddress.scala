package io.scalac.tezos.translator.model

import javax.mail.internet.InternetAddress

import scala.util.Try


sealed abstract case class EmailAddress(value: InternetAddress) {
  override def toString: String = value.getAddress
}

object EmailAddress {

  def fromString(s: String): Try[EmailAddress] =
    Try(new InternetAddress(s, true))
      .map(ia => new EmailAddress(ia) {})
}
