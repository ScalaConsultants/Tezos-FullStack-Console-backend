package io.scalac.tezos.translator.model

import io.scalac.tezos.translator.model.types.Auth.{UserToken, Username}

case class AuthUserData(username: Username, token: UserToken)
