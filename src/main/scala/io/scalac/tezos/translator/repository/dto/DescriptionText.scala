package io.scalac.tezos.translator.repository.dto

import javax.mail.internet.InternetAddress

import scala.util.Try


sealed abstract case class DescriptionText(value: InternetAddress) {
  override def toString: String = value.getAddress
}

object DescriptionText {

  def fromString(s: String): Try[DescriptionText] =
    Try(new InternetAddress(s, true))
      .map(ia => new DescriptionText(ia) {})
}
