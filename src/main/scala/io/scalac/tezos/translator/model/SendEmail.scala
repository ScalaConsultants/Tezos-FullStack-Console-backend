package io.scalac.tezos.translator.model

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.scalac.tezos.translator.model.SendEmail._

import scala.util.Try

case class SendEmail(
  uid: Uid,
  from: EmailAddress,
  to: EmailAddress,
  subject: String,
  content: Content
)

object SendEmail {
  sealed trait Content extends Product with Serializable

  case class ContactFormContent(
    name: String,
    phone: String,
    email: String,
    content: String
  ) extends Content

  case class TextContent(msg: String) extends Content

  object Content {
    def toJson(c: Content): String = c.asJson.noSpaces
    def fromJson(s: String): Try[Content] = decode[Content](s).toTry

    def toPrettyString(c: Content): String = c match {
      case c: ContactFormContent =>
        s"""
           |name: ${c.name}
           |phone: ${c.phone}
           |email: ${c.email}
           |content: ${c.content}
           |""".stripMargin

      case t: TextContent => t.msg
    }
  }

  sealed trait EmailAddress extends Product with Serializable
  case object AdminEmail extends EmailAddress
  case object MmtServiceEmail extends EmailAddress
  case class UserEmail(v: String) extends EmailAddress

  object EmailAddress {
    def toString(a: EmailAddress): String = a match {
      case AdminEmail => "admin"
      case MmtServiceEmail => "mmt_service"
      case UserEmail(v) => v
    }

    def fromString(s: String): EmailAddress = s match {
      case "admin" => AdminEmail
      case "mmt_service" => MmtServiceEmail
      case other => UserEmail(other)
    }
  }
}
