package io.scalac.tezos.translator.model

import org.joda.time.DateTime

case class LibraryModel(id: Long,
                        name: String,
                        author: String,
                        description: String,
                        micheline: String,
                        michelson: String,
                        createdAt: DateTime,
                        status: Option[Int])
