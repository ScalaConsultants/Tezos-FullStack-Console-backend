package io.scalac.tezos.translator.routes.utils

import io.scalac.tezos.translator.micheline.MichelineTranslator
import io.scalac.tezos.translator.michelson.JsonToMichelson
import io.scalac.tezos.translator.michelson.dto.MichelsonSchema

trait Translator {
  def michelson2micheline(input: String): Either[String, String]
  def micheline2michelson(input: String): Either[Throwable, String]
}

case object MMTranslator extends Translator {
  def michelson2micheline(input: String): Either[String, String]    = MichelineTranslator.michelsonToMicheline(input)
  def micheline2michelson(input: String): Either[Throwable, String] = JsonToMichelson.convert[MichelsonSchema](input)
}
