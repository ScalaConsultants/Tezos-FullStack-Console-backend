package io.scalac.tezos.translator.model

import akka.http.scaladsl.unmarshalling.Unmarshaller

object Translation {

  sealed trait From

  case object FromMicheline extends From
  case object FromMichelson extends From

  def asString(r: From) = r match {
    case FromMicheline => "micheline"
    case FromMichelson => "michelson"
  }

  def fromString(s: String): Option[From] = s.toLowerCase match {
    case "micheline" => Some(FromMicheline)
    case "michelson" => Some(FromMichelson)
    case _ => None
  }

  implicit val fromDeserializer = Unmarshaller.strict[String, From] { string =>
    string.toLowerCase match {
      case "micheline" => FromMicheline
      case "michelson" => FromMichelson
      case x => throw new IllegalArgumentException(s"'$x' is not a valid `source` value")
    }
  }
}
