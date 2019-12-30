package io.scalac.tezos.translator

import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineMV
import io.scalac.tezos.translator.model.UserCredentials
import io.scalac.tezos.translator.model.types.Auth.{Password, Username, UsernameReq}

object Helper {
   def testFormat(s: String): String = s.replaceAll("\n", "").replaceAll("\r", "")

  val adminUsername: Username = Username(refineMV[UsernameReq]("admin"))
  val adminPassword: Password = Password(refineMV[NonEmpty]("zxcv"))
  val adminCredentials: UserCredentials = UserCredentials(adminUsername, adminPassword)

}
