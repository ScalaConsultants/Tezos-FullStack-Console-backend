package io.scalac.tezos.translator

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ForEachTestContainer, MySQLContainer}
import io.circe.parser._
import io.scalac.tezos.translator.config.Configuration
import io.scalac.tezos.translator.repository.{Emails2SendRepository, LibraryRepository}
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import slick.jdbc.MySQLProfile

class TranslationTest extends WordSpec with MustMatchers with ScalaFutures with ScalatestRouteTest with ForEachTestContainer {
  override lazy val container = MySQLContainer(mysqlImageVersion = DbTestBase.mySqlVersion)

  private trait DatabaseFixture extends DbTestBase {
    implicit val testDb: MySQLProfile.backend.Database = DbTestBase.dbFromContainer(container)

    val email2SendService = new Emails2SendService(emails2SendRepo, testDb)
    val libraryService    = new LibraryService(libraryRepo, testDb)

    val routes: Route = new Routes(email2SendService, libraryService, null, log, config).allRoutes

    recreateTables()
  }

  val emails2SendRepo = new Emails2SendRepository
  val libraryRepo     = new LibraryRepository

  val log: LoggingAdapter = system.log
  val config: Configuration = Configuration.getConfig(log)


  "A Routes" can {
    "translate michelson to micheline" when {
      "michelson is correct" in new DatabaseFixture {
        Post("/v1/translate/from/michelson/to/micheline", Samples.michelson) ~> routes ~> check {
          status must equal(StatusCodes.OK)
          parse(responseAs[String]) mustEqual parse(Samples.micheline)
        }
      }

      "michelson in incorrect 1" in new DatabaseFixture {
        Post("/v1/translate/from/michelson/to/micheline", Samples.incorrectMichelson1) ~> routes ~> check {
          status must equal(StatusCodes.BadRequest)
        }
      }

      "michelson in incorrect 2" in new DatabaseFixture {
        Post("/v1/translate/from/michelson/to/micheline", Samples.incorrectMichelson1) ~> routes ~> check {
          status must equal(StatusCodes.BadRequest)
        }
      }
    }

    "translate micheline to michelson" when {

      "micheline is correct" in new DatabaseFixture {
        Post("/v1/translate/from/micheline/to/michelson", Samples.micheline) ~> routes ~> check {
          status must equal(StatusCodes.OK)
          parse(responseAs[String]) mustEqual parse(Samples.michelson)
        }
      }

      "micheline is incorrect 1" in new DatabaseFixture {
        Post("/v1/translate/from/micheline/to/michelson", Samples.incorrectMicheline1) ~> routes ~> check {
          status must equal(StatusCodes.BadRequest)
        }
      }

      "micheline is incorrect 2" in new DatabaseFixture {
        Post("/v1/translate/from/micheline/to/michelson", Samples.incorrectMicheline2) ~> routes ~> check {
          status must equal(StatusCodes.BadRequest)
        }
      }

    }
  }

}
