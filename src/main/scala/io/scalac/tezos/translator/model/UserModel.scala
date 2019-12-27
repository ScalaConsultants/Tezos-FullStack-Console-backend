package io.scalac.tezos.translator.model

import io.scalac.tezos.translator.model.types.Auth.Username

case class UserModel(username: Username,
                     passwordHash: String)
