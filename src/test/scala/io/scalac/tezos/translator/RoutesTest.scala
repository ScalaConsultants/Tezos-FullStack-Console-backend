package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures

class RoutesTest extends WordSpec with MustMatchers with ScalaFutures with ScalatestRouteTest {

  val route = Routes.route

  "A Routes" can {
    "translate michelson to micheline" when {
      "michelson is correct" in {
        Post("/v1/translate/from/michelson/to/micheline", Samples.michelson) ~> route ~> check {
          status must equal(StatusCodes.OK)
        }
      }

      "michelson in incorrect" in {
        Post("/v1/translate/from/michelson/to/micheline", Samples.incorrectMichelson) ~> route ~> check {
          status must equal(StatusCodes.BadRequest)
        }
      }
    }

    "translate micheline to michelson" when {

      "micheline is correct" in {
        Post("/v1/translate/from/micheline/to/michelson", Samples.micheline) ~> route ~> check {
          status must equal(StatusCodes.OK)
        }
      }

      "micheline is incorrect" in {
        Post("/v1/translate/from/micheline/to/michelson", Samples.incorrectMicheline) ~> route ~> check {
          status must equal(StatusCodes.BadRequest)
        }
      }

    }
  }

}
