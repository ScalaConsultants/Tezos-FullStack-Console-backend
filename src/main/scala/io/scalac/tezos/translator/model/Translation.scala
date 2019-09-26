package io.scalac.tezos.translator.model

import akka.http.scaladsl.unmarshalling.Unmarshaller

object Translation {

  sealed trait From

  case object FromMicheline extends From
  case object FromMichelson extends From

  implicit val fromDeserializer = Unmarshaller.strict[String, From] { string =>
    string.toLowerCase match {
      case "micheline" => FromMicheline
      case "michelson" => FromMichelson
      case x => throw new IllegalArgumentException(s"'$x' is not a valid `source` value")
    }
  }
}
