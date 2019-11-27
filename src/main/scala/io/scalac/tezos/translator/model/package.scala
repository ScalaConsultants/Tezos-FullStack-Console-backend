package io.scalac.tezos.translator

import java.sql.Timestamp

import org.joda.time.DateTime

package object model {

  case class CaptchaVerifyResponse(
    success:       Boolean,
    challenge_ts:  Option[DateTime],
    hostname:      Option[String],
    `error-codes`: Option[List[String]]
  )
  case class Error(error: String)
  case class Errors(errors: List[String])

  case class SendEmailJsonDTO(
    name: String,
    phone: String,
    email: String,
    content: String
  )

  case class SendEmailDbDTO(
    id: Option[Long],
    uid: String,
    name: String,
    phone: String,
    email: String,
    content: String,
    createdAt: Timestamp
  )

  case class LibraryJsonDTO(
    name: String,
    author: String,
    email: Option[String],
    description: String,
    micheline: String,
    michelson: String
  )

  case class LibraryDbDTO(
    id: Option[Long],
    uid: String,
    name: String,
    author: String,
    email: Option[String],
    description: String,
    micheline: String,
    michelson: String,
    createdAt: Timestamp,
    status: Int = 1
  )

}
