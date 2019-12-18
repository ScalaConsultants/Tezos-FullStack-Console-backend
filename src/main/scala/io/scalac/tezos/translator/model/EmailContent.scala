package io.scalac.tezos.translator.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

import scala.util.Try

sealed trait EmailContent extends Product with Serializable

case class ContactFormContent(
  name: String,
  contact:Contact,
  content: String
) extends EmailContent

case class TextContent(msg: String) extends EmailContent

object EmailContent {


  implicit val contactFormContentDecoder: Decoder[ContactFormContent] = Decoder.forProduct3("name","contact","content")(ContactFormContent.apply)
  implicit val contactFormContentEncoder: Encoder[ContactFormContent] = Encoder.forProduct3("name","contact","content")(u=> (u.name,u.contact,u.content))
  implicit val textContentDecoder: Decoder[TextContent] = Decoder.forProduct1("msg")(TextContent.apply)
  implicit val textContentEncoder: Encoder[TextContent] = Encoder.forProduct1("msg")(u=> (u.msg))

  def toJson(c: EmailContent): String = c.asJson.noSpaces
  def fromJson(s: String): Try[EmailContent] = decode[EmailContent](s).toTry

  def toPrettyString(c: EmailContent): String = c match {
    case c: ContactFormContent =>
      val s = c.contact match {
        case ContactPhone(v) => Seq(s"phone: $v")
        case ContactEmail(v) => Seq(s"email: $v")
        case FullContact(a, b) => Seq(s"phone: $a", s"email: $b")
      }

      s"""
         |name: ${c.name}
         |
         |email: ${c.contact.getEmail()}
         |content: ${c.content}
         |""".stripMargin

    case t: TextContent => t.msg
  }
}
