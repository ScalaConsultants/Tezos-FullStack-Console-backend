package io.scalac.tezos.translator

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.parser._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.fakes.FakeMMTranslator
import io.scalac.tezos.translator.routes.TranslatorRoutes
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ MustMatchers, WordSpec }

class TranslationTest extends WordSpec with MustMatchers with ScalaFutures with ScalatestRouteTest {

  val log: LoggingAdapter   = system.log
  val config: CaptchaConfig = CaptchaConfig()
  val routes: Route         = new TranslatorRoutes(FakeMMTranslator).routes

  "A Routes" can {
    "translate michelson to micheline" when {
      "michelson is correct" in {
        Post("/v1/translate/from/michelson/to/micheline", Samples.michelson) ~> routes ~> check {
          status must equal(StatusCodes.OK)
          parse(responseAs[String]) mustEqual parse(Samples.micheline)
        }
      }

      "michelson in incorrect 1" in {
        Post("/v1/translate/from/michelson/to/micheline", Samples.incorrectMichelson1) ~> routes ~> check {
          status must equal(StatusCodes.BadRequest)
        }
      }

      "michelson in incorrect 2" in {
        Post("/v1/translate/from/michelson/to/micheline", Samples.incorrectMichelson1) ~> routes ~> check {
          status must equal(StatusCodes.BadRequest)
        }
      }
    }

    "translate micheline to michelson" when {

      "micheline is correct" in {
        Post("/v1/translate/from/micheline/to/michelson", Samples.micheline) ~> routes ~> check {
          status must equal(StatusCodes.OK)
          parse(responseAs[String]) mustEqual parse(Samples.michelson)
        }
      }

      "micheline is incorrect 1" in {
        Post("/v1/translate/from/micheline/to/michelson", Samples.incorrectMicheline1) ~> routes ~> check {
          status must equal(StatusCodes.BadRequest)
        }
      }

      "micheline is incorrect 2" in {
        Post("/v1/translate/from/micheline/to/michelson", Samples.incorrectMicheline2) ~> routes ~> check {
          status must equal(StatusCodes.BadRequest)
        }
      }

    }
  }

}
