package io.scalac.tezos.translator.model

import org.joda.time.DateTime

case class SendEmailModel(id: Long,
                          name: String,
                          phone: String,
                          inSending: Boolean,
                          createdAt: DateTime,
                          lastTryToSend: Option[DateTime])