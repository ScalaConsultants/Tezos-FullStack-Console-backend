package io.scalac.tezos.translator

import io.scalac.tezos.translator.model.{History, Translation}

import scala.collection.mutable

class TranslationsService {

  private val maxReturnSize: Int = 10
  private val maxSize: Int = 100

  private val translations = mutable.MutableList[History]()

  def addTranslation(from: Translation.From, source: String, translation: String): Unit = {
    translations += model.History(from, source, translation)

    if(translations.size > maxSize) {
      translations.dropRight(translations.size - maxSize)
    }
  }

  def list(fromFilter: Option[Translation.From]): List[History] =
    fromFilter.map { from =>
      translations.filter(_.from == from).take(maxReturnSize).toList
    }.getOrElse(
      translations.take(maxReturnSize).toList
    )

}
