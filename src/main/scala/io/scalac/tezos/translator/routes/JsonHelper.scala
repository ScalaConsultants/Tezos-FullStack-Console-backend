package io.scalac.tezos.translator.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.scalac.tezos.translator.model._
import io.scalac.tezos.translator.routes.dto.{LibraryEntryRoutesAdminDto, LibraryEntryRoutesDto, SendEmailRoutesDto}
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
  lazy implicit val libraryDTOFormat: RootJsonFormat[LibraryEntryRoutesDto] = jsonFormat6(LibraryEntryRoutesDto.apply)
  lazy implicit val libraryAdminDTOFormat: RootJsonFormat[LibraryEntryRoutesAdminDto] = jsonFormat8(LibraryEntryRoutesAdminDto.apply)
  lazy implicit val sendEmailDTOFormat: RootJsonFormat[SendEmailRoutesDto] = jsonFormat4(SendEmailRoutesDto.apply)
  lazy implicit val userCredentialsDTOFormat:RootJsonFormat[UserCredentials] = jsonFormat2(UserCredentials)
  lazy implicit val captchaVerifyResponseFormat: RootJsonFormat[CaptchaVerifyResponse] = jsonFormat4(CaptchaVerifyResponse)

}
