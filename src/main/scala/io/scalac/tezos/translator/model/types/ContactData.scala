package io.scalac.tezos.translator.model.types

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.{And, Not}
import eu.timepit.refined.collection.{NonEmpty, Size}
import eu.timepit.refined.numeric.Greater
import eu.timepit.refined.string.MatchesRegex
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import io.scalac.tezos.translator.model.types.Util._
import sttp.tapir.{Schema, SchemaType}

object ContactData {

  type NameAndEmailReq = NonEmpty And Size[Not[Greater[W.`255`.T]]]

  type PhoneReq = NonEmpty And MatchesRegex[W.`"""^\\+?\\d{6,18}$"""`.T]

  @newtype case class Name(v: String Refined NameAndEmailReq)

  @newtype case class Phone(v: String Refined PhoneReq)

  @newtype case class RefinedEmailString(v: String Refined NameAndEmailReq)

  @newtype case class Content(v: String Refined NonEmpty)

  implicit val nameEncoder: Encoder[Name] = buildToStringEncoder
  implicit val nameDecoder: Decoder[Name] =
    buildStringRefinedDecoder[Name, NameAndEmailReq]("Can't decode name", Name.apply)

  implicit val phoneEncoder: Encoder[Phone] = buildToStringEncoder
  implicit val phoneDecoder: Decoder[Phone] =
    buildStringRefinedDecoder[Phone, PhoneReq]("Can't decode phone", Phone.apply)

  implicit val refinedEmailStringEncoder: Encoder[RefinedEmailString] = buildToStringEncoder
  implicit val refinedEmailStringDecoder: Decoder[RefinedEmailString] =
    buildStringRefinedDecoder[RefinedEmailString, NameAndEmailReq]("Can't decode email", RefinedEmailString.apply)

  implicit val contentEncoder: Encoder[Content] = buildToStringEncoder
  implicit val contentDecoder: Decoder[Content] =
    buildStringRefinedDecoder[Content, NonEmpty]("Can't decode content", Content.apply)

  implicit val nameSchema: Schema[Name] = new Schema[Name](SchemaType.SString, false)
  implicit val phoneSchema: Schema[Phone] = new Schema[Phone](SchemaType.SString, true)
  implicit val refinedEmailSchema: Schema[RefinedEmailString] = new Schema[RefinedEmailString](SchemaType.SString, true)
  implicit val contentSchema: Schema[Content] = new Schema[Content](SchemaType.SString, false)

}
