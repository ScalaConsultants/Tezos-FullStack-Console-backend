package io.scalac.tezos.translator.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.scalac.tezos.translator.TranslationsService
import io.scalac.tezos.translator.model.{HistoryViewModel, Translation}
import io.scalac.tezos.translator.model.HistoryViewModelExtension._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val historyViewModelFormat = jsonFormat2(HistoryViewModel)
}

class HistoryRoutes(translationsService: TranslationsService) extends HttpRoutes with JsonSupport {

  val defaultResultLimit: Int = 10

  override def routes =
    pathPrefix("translations") {
      pathEndOrSingleSlash {
        get {
          parameters('source.as[Translation.From].?, 'limit.as[Int].?) { (sourceOpt, limitOpt) =>
            val result = for {
              translations <- translationsService.list(sourceOpt, limitOpt.getOrElse(defaultResultLimit))
            } yield translations.map(_.toViewModel)
            complete(result)
          }
        }
      }
    }
}
