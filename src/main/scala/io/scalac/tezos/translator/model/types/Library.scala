package io.scalac.tezos.translator.model.types

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.{And, Not}
import eu.timepit.refined.collection.{NonEmpty, Size}
import eu.timepit.refined.numeric.Greater
import io.estatico.newtype.macros.newtype
import Util._
import io.circe.{Decoder, Encoder}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import sttp.tapir.{Schema, SchemaType}

object Library {

  type NotEmptyAndNotLong = NonEmpty And Size[Not[Greater[W.`255`.T]]]

  @newtype case class Author(v: String Refined NotEmptyAndNotLong)

  @newtype case class Description(v: String Refined NotEmptyAndNotLong)

  @newtype case class Micheline(v: String Refined NonEmpty)

  @newtype case class Michelson(v: String Refined NonEmpty)

  @newtype case class Title(v: String Refined NotEmptyAndNotLong)

  implicit val authorMapper: JdbcType[Author] with BaseTypedType[Author] =
    buildRefinedStringMapper(Author.apply)

  implicit val descriptionMapper: JdbcType[Description] with BaseTypedType[Description] =
    buildRefinedStringMapper(Description.apply)

  implicit val michelineMapper: JdbcType[Micheline] with BaseTypedType[Micheline] =
    buildRefinedStringMapper(Micheline.apply)

  implicit val michelsonMapper: JdbcType[Michelson] with BaseTypedType[Michelson] =
    buildRefinedStringMapper(Michelson.apply)

  implicit val titleMapper: JdbcType[Title] with BaseTypedType[Title] =
    buildRefinedStringMapper(Title.apply)

  implicit val authorEncoder: Encoder[Author] = buildToStringEncoder
  implicit val authorDecoder: Decoder[Author] =
    buildStringRefinedDecoder("Can't parse author", Author.apply)

  implicit val descriptionEncoder: Encoder[Description] = buildToStringEncoder
  implicit val descriptionDecoder: Decoder[Description] =
    buildStringRefinedDecoder("Can't parse description", Description.apply)

  implicit val michelineEncoder: Encoder[Micheline] = buildToStringEncoder
  implicit val michelineDecoder: Decoder[Micheline] =
    buildStringRefinedDecoder("Can't parse micheline", Micheline.apply)

  implicit val michelsonEncoder: Encoder[Michelson] = buildToStringEncoder
  implicit val michelsonDecoder: Decoder[Michelson] =
    buildStringRefinedDecoder("Can't parse michelson", Michelson.apply)

  implicit val titleEncoder: Encoder[Title] = buildToStringEncoder
  implicit val titleDecoder: Decoder[Title] =
    buildStringRefinedDecoder("Can't parse title", Title.apply)

  implicit val authorSchema: Schema[Author] = new Schema[Author](SchemaType.SString, false)
  implicit val descriptionSchema: Schema[Description] = new Schema[Description](SchemaType.SString, false)
  implicit val michelineSchema: Schema[Micheline] = new Schema[Micheline](SchemaType.SString, false)
  implicit val michelsonSchema: Schema[Michelson] = new Schema[Michelson](SchemaType.SString, false)
  implicit val titleSchema: Schema[Title] = new Schema[Title](SchemaType.SString, false)



}
