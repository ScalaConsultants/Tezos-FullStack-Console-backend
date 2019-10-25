package io.scalac.tezos.translator.model

import org.joda.time.DateTime

case class TranslationDomainModel(id: Option[Long],
                                  from: Translation.From,
                                  source: String,
                                  translation: String,
                                  createdAt: DateTime)
