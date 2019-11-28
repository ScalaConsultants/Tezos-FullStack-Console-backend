package io.scalac.tezos.translator

import io.scalac.tezos.translator.routes.util.Translator

object Fakes {
  val MMTranslatorFake: Translator = new Translator {

    override def michelson2micheline(input: String): Either[String, String] =
      input match {
        case Samples.michelson => Right(Samples.micheline)
        case Samples.incorrectMichelson1 => Left("incorrect Michelson 1")
        case Samples.incorrectMichelson2 => Left("incorrect Michelson 2")
        case _ => Left("incorrect input")
      }

    override def micheline2michelson(input: String): Either[Throwable, String] =
      input match {
        case Samples.micheline => Right(Samples.michelson)
        case Samples.incorrectMicheline1 => Left(new Exception("incorrect Micheline 1"))
        case Samples.incorrectMicheline2 => Left(new Exception("incorrect Micheline 2"))
        case _ => Left(new Exception("incorrect input"))
      }
  }
}
