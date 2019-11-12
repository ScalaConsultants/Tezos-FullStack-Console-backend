package io.scalac.tezos.translator.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.scalac.tezos.translator.model._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait JsonHelper extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {

    private val parserISO : DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis

    override def write(obj: DateTime) = JsString(parserISO.print(obj))

    override def read(json: JsValue) : DateTime = json match {
      case JsString(s) => parserISO.parseDateTime(s)
      case _ => throw DeserializationException("Can't parse string fot datetime")
    }
  }

  lazy implicit val errorDTOFormat: RootJsonFormat[Error] = jsonFormat1(Error)
  lazy implicit val errorsDTOFormat: RootJsonFormat[Errors] = jsonFormat1(Errors)
  lazy implicit val libraryDTOFormat: RootJsonFormat[LibraryDTO] = jsonFormat5(LibraryDTO)
  lazy implicit val sendEmailDTOFormat: RootJsonFormat[SendEmailDTO] = jsonFormat4(SendEmailDTO)
  lazy implicit val captchaVerifyResponseFormat: RootJsonFormat[CaptchaVerifyResponse] = jsonFormat4(CaptchaVerifyResponse)

}
