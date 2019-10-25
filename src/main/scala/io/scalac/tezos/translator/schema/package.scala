package io.scalac.tezos.translator

import java.sql.Timestamp

import io.scalac.tezos.translator.model.Translation
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._

package object schema {

  implicit val datetimeColumnType = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.toDate.getTime),
    ts => new DateTime(ts)
  )

  implicit val translationFromColumnType =
    MappedColumnType.base[Translation.From, String](
      Translation.asString,
      { x =>
        Translation.fromString(x).getOrElse(throw new Exception("invalid translation format: " + x))
      }
    )
}
