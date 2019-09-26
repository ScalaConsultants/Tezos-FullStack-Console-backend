package io.scalac.tezos.translator

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.scalac.tezos.translator.micheline.MichelineTranslator
import io.scalac.tezos.translator.model.HistoryViewModel
import io.scalac.tezos.translator.routes.JsonSupport
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class HistoryTest extends FlatSpec with Matchers with ScalatestRouteTest with JsonSupport with BeforeAndAfterAll {

  val routes = new Routes(new TranslationsService).allRoutes

  override def beforeAll() = {
    translateMicheline(Samples.micheline)
    translateMichelson(Samples.michelson)
    translateMichelson(Samples.micheline, StatusCodes.BadRequest)
    translateMicheline(Samples.micheline)
  }

  val michelsonTranslation = // WORKAROUND: translation is formatted differently than the source
    MichelineTranslator
      .michelsonToMicheline(Samples.michelson)
      .getOrElse("Something goes wrong")

  "A History" should "return translations history" in {
    Get("/v1/translations") ~> routes ~> check {
      status shouldBe StatusCodes.OK

      val history = responseAs[List[HistoryViewModel]]
      history should have size 3

      val expectedSources = List(Samples.micheline, Samples.michelson, Samples.micheline)
      val expectedTranslations = List(Samples.michelson, michelsonTranslation, Samples.michelson)

      history.map(_.source) shouldBe expectedSources
      history.map(_.translation) shouldBe expectedTranslations
    }
  }

  it should "fails for unknown source" in {
    Get("/v1/translations?source=lorem") ~> Route.seal(routes) ~> check {
      status shouldBe StatusCodes.BadRequest
      responseAs[String] shouldBe "The query parameter 'source' was malformed:\n'lorem' is not a valid `source` value"
    }
  }

  it should "return only micheline to michelson translations" in {
    Get("/v1/translations?source=micheline") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val history = responseAs[List[HistoryViewModel]]
      history should have size 2

      val expectedSources = List(Samples.micheline, Samples.micheline)
      val expectedTranslations = List(Samples.michelson, Samples.michelson)

      history.map(_.source) shouldBe expectedSources
      history.map(_.translation) shouldBe expectedTranslations
    }
  }

  it should "return only michelson to micheline translations" in {
    Get("/v1/translations?source=michelson") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val history = responseAs[List[HistoryViewModel]]
      history should have size 1

      history.map(_.source) shouldBe List(Samples.michelson)
      history.map(_.translation) shouldBe List(michelsonTranslation)
    }
  }

  it should "return only 10 history entries" in {
    (1 to 10) foreach { _ =>
      translateMichelson(Samples.michelson)
      translateMicheline(Samples.micheline)
    }
    Get("/v1/translations") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val history = responseAs[List[HistoryViewModel]]
      history should have size 10

      val expectedSources = Set(Samples.micheline, Samples.michelson)
      history.map(_.source).toSet shouldBe expectedSources
    }
  }

  it should "return only 10 history entries filtered by micheline" in {
    (1 to 10) foreach { _ =>
      translateMichelson(Samples.michelson)
      translateMicheline(Samples.micheline)
    }
    Get("/v1/translations?source=micheline") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val history = responseAs[List[HistoryViewModel]]
      history should have size 10
      history.map(_.source).toSet shouldBe Set(Samples.micheline)
    }
  }

  it should "return only 10 history entries filtered by michelson" in {
    (1 to 10) foreach { _ =>
      translateMichelson(Samples.michelson)
      translateMicheline(Samples.micheline)
    }
    Get("/v1/translations?source=michelson") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val history = responseAs[List[HistoryViewModel]]
      history should have size 10

      history.map(_.source).toSet shouldBe Set(Samples.michelson)
    }
  }

  private def translateMicheline(micheline: String) =
    Post("/v1/translate/from/micheline/to/michelson", micheline) ~> routes ~> check {
      status shouldBe StatusCodes.OK
    }

  private def translateMichelson(michelson: String, statusCode: StatusCode = StatusCodes.OK) =
    Post("/v1/translate/from/michelson/to/micheline", michelson) ~> routes ~> check {
      status shouldBe statusCode
    }
}
