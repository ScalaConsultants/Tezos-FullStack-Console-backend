package io.scalac.tezos.translator.model

import io.scalac.tezos.translator.model.types.Auth.{ Password, Username }

case class UserCredentials(username: Username, password: Password)
