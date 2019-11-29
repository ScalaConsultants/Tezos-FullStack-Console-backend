package io.scalac.tezos.translator

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

}
