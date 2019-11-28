package io.scalac.tezos.translator.model

case class SendEmail(
  uid: Uid,
  name: String,
  phone: String,
  email: String,
  content: String
)
