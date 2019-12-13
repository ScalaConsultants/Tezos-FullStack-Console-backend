package io.scalac.tezos.translator.model

import javax.mail.internet.InternetAddress

import scala.util.Try

sealed abstract case class AuthorName(value: InternetAddress) {
  override def toString: String = value.getAddress
}

object AuthorName {

  def fromString(s: String): Try[AuthorName] =
    Try(new InternetAddress(s, true))
      .map(ia => new AuthorName(ia) {})
}
