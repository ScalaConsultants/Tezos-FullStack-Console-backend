package io.scalac.tezos.translator.model

import io.scalac.tezos.translator.model.types.Auth.{PasswordHash, Username}

case class UserModel(username: Username, passwordHash: PasswordHash)
