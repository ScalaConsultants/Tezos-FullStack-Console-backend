package io.scalac.tezos.translator

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.parser._
import io.scalac.tezos.translator.config.Configuration
import io.scalac.tezos.translator.repository.{Emails2SendRepository, LibraryRepository, TranslationRepository}
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, TranslationsService}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

class TranslationTest extends WordSpec with MustMatchers with ScalaFutures with ScalatestRouteTest {

  implicit val repository: TranslationRepository = new TranslationRepository
  val emails2SendRepo = new Emails2SendRepository
  val libraryRepo     = new LibraryRepository

  implicit val db: MySQLProfile.backend.Database = Database.forConfig("tezos-db")

  private val service = new TranslationsService
  val email2SendService = new Emails2SendService(emails2SendRepo, db)
  val libraryService    = new LibraryService(libraryRepo, db)
  val log: LoggingAdapter = system.log
  val config: Configuration = Configuration.getConfig(log)

  val routes: Route = new Routes(service, email2SendService, libraryService, log, config).allRoutes

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
