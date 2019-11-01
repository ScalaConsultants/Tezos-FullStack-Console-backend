package io.scalac.tezos.translator.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.scalac.tezos.translator.model.dto.SendEmailDTO
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonHelper extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val sendEmailDTOFormat: RootJsonFormat[SendEmailDTO] = jsonFormat4(SendEmailDTO)

}
