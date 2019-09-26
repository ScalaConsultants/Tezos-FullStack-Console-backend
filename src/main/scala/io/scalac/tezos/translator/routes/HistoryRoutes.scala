package io.scalac.tezos.translator.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.scalac.tezos.translator.TranslationsService
import io.scalac.tezos.translator.model.{HistoryViewModel, Translation}
import io.scalac.tezos.translator.model.HistoryViewModelExtension._
import spray.json._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val historyViewModelFormat = jsonFormat2(HistoryViewModel)
}

class HistoryRoutes(translationsService: TranslationsService) extends HttpRoutes with JsonSupport {

  override def routes =
    pathPrefix("translations") {
      pathEndOrSingleSlash {
        get {
          parameters('source.as[Translation.From].?) { sourceOpt =>
            complete(translationsService.list(sourceOpt).map(_.toViewModel))
          }
        }
      }
    }
}
