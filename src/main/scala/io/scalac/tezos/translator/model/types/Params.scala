package io.scalac.tezos.translator.model.types

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import io.estatico.newtype.macros.newtype
import sttp.tapir.Codec
import sttp.tapir.CodecFormat.TextPlain
import Util._

object Params {

  @newtype case class Limit(v: Int Refined Positive)

  @newtype case class Offset(v: Int Refined Positive)

  implicit val limitStringCodec: Codec[Limit, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(buildDecoderWithPositiveInt[Limit](_, Limit.apply))(encodeToString(_))

  implicit val offsetStringCodec: Codec[Offset, TextPlain, String] = Codec.stringPlainCodecUtf8
    .mapDecode(buildDecoderWithPositiveInt[Offset](_, Offset.apply))(encodeToString(_))

}
