package io.scalac.tezos.translator.model

import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

import scala.util.Try

sealed trait EmailContent extends Product with Serializable

case class ContactFormContent(name: String, contact: Contact, content: String) extends EmailContent

case class TextContent(msg: String) extends EmailContent

object EmailContent {

  def toJson(c: EmailContent): String        = c.asJson.noSpaces
  def fromJson(s: String): Try[EmailContent] = decode[EmailContent](s).toTry

  def toPrettyString(c: EmailContent): String = c match {
    case c: ContactFormContent =>
      s"""
         |name: ${c.name}
         |${Contact.prettyString(c.contact)}
         |content: ${c.content}
         |""".stripMargin

    case t: TextContent => t.msg
  }
}
