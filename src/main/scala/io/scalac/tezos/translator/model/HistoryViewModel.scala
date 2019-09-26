package io.scalac.tezos.translator.model

case class HistoryViewModel(source: String, translation: String)

object HistoryViewModelExtension {

  implicit class HistoryExtension(history: History) {

    def toViewModel: HistoryViewModel = HistoryViewModel(history.source, history.translation)
  }

}
