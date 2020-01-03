package io.scalac.tezos.translator.model.types

import eu.timepit.refined.{W, refineV}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.{And, Not}
import eu.timepit.refined.collection.{NonEmpty, Size}
import eu.timepit.refined.numeric.Greater
import eu.timepit.refined.string.MatchesRegex
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import io.scalac.tezos.translator.model.EmailAddress
import io.scalac.tezos.translator.model.types.Util._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import sttp.tapir.{Schema, SchemaType}

object ContactData {

  type EmailReq = MatchesRegex[W.`"""^\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$"""`.T]

  type NameReq = NonEmpty And Size[Not[Greater[W.`255`.T]]]

  type PhoneReq = NonEmpty And MatchesRegex[W.`"""^\\+?\\d{6,18}$"""`.T]

  @newtype case class Name(v: String Refined NameReq)

  @newtype case class Phone(v: String Refined PhoneReq)

  @newtype case class EmailS(v: String Refined EmailReq)

  @newtype case class Content(v: String Refined NonEmpty)

  implicit val nameEncoder: Encoder[Name] = buildToStringEncoder
  implicit val nameDecoder: Decoder[Name] =
    buildStringRefinedDecoder[Name, NameReq]("Can't decode name", Name.apply)

  implicit val phoneEncoder: Encoder[Phone] = buildToStringEncoder
  implicit val phoneDecoder: Decoder[Phone] =
    buildStringRefinedDecoder[Phone, PhoneReq]("Can't decode phone", Phone.apply)

  implicit val refinedEmailStringEncoder: Encoder[EmailS] = buildToStringEncoder
  implicit val refinedEmailStringDecoder: Decoder[EmailS] =
    buildStringRefinedDecoder[EmailS, EmailReq]("Can't decode email", EmailS.apply)

  implicit val contentEncoder: Encoder[Content] = buildToStringEncoder
  implicit val contentDecoder: Decoder[Content] =
    buildStringRefinedDecoder[Content, NonEmpty]("Can't decode content", Content.apply)

  implicit val nameSchema: Schema[Name]           = new Schema[Name](SchemaType.SString, false)
  implicit val phoneSchema: Schema[Phone]         = new Schema[Phone](SchemaType.SString, true)
  implicit val refinedEmailSchema: Schema[EmailS] = new Schema[EmailS](SchemaType.SString, true)
  implicit val contentSchema: Schema[Content]     = new Schema[Content](SchemaType.SString, false)

  implicit val emailSMapper: JdbcType[EmailS] with BaseTypedType[EmailS] = buildRefinedStringMapper(EmailS.apply)

  protected def emailRefinedFromMail(email: EmailAddress): Option[EmailS] =
    refineV[EmailReq](email.toString).map(EmailS.apply).toOption

  implicit class MaybeEmailAddressOps(val maybeAddress: Option[EmailAddress]) extends AnyVal {
    def toEmailS: Option[EmailS] = maybeAddress.flatMap(emailRefinedFromMail)
  }

}
