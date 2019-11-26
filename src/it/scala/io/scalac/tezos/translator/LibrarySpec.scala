package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ForEachTestContainer, MySQLContainer}
import io.scalac.tezos.translator.config.{CaptchaConfig, Configuration}
import io.scalac.tezos.translator.model.{Errors, LibraryDTO}
import io.scalac.tezos.translator.repository.LibraryRepository
import io.scalac.tezos.translator.routes.{JsonHelper, LibraryRoutes}
import io.scalac.tezos.translator.schema.LibraryTable
import io.scalac.tezos.translator.service.LibraryService
import org.scalatest.{Assertion, BeforeAndAfterAll, Matchers, WordSpec}
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class LibrarySpec
  extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with BeforeAndAfterAll
  with JsonHelper
  with ForEachTestContainer {
    override lazy val container = MySQLContainer()

    private trait DatabaseFixture extends DbTestBase {
      val testDb: MySQLProfile.backend.Database = DbTestBase.dbFromContainer(container)

      val libraryService = new LibraryService(libraryRepo, testDb)
      val libraryRoute: Route = new LibraryRoutes(libraryService, system.log, config).routes

      recreateTables()

      def insertDummiesToDb(size: Int): immutable.IndexedSeq[Int] = {
        for (_ <- 1 to size)
          yield
            runDB(dbAction =
              sqlu"""insert into library (name, author, description, micheline, michelson, status) values
                  ('some', 'some', 'some', 'some', 'some', 1)"""
            )
      }

      def checkValidationErrorsWithExpected(dto: LibraryDTO, expectedErrors: List[String]): Assertion = {
        Post("/library", dto) ~> Route.seal(libraryRoute) ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[Errors].errors should contain theSameElementsAs expectedErrors
        }
      }
    }

    val reCaptchaConfig = CaptchaConfig(checkOn = false, "", "", "")
    val config          = Configuration(reCaptcha = reCaptchaConfig)
    val libraryRepo = new LibraryRepository
    val longField: String = "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop" +
      "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqw" +
      "ertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop"

    "Library routes" should {
      "validate payload before storing" in new DatabaseFixture {
        val emptyPayload = LibraryDTO("", "", "", "", "")
        val expectedErrors1 = List("name field is empty", "author field is empty", "description field is empty",
          "micheline field is empty", "michelson field is empty")

        checkValidationErrorsWithExpected(emptyPayload, expectedErrors1)

        val toLongPayload = LibraryDTO(longField, longField, "description", "some", "some")
        val expectedErrors2 = List("field name is too long, max length - 255", "field author is too long, max length - 255")

        checkValidationErrorsWithExpected(toLongPayload, expectedErrors2)
      }

      "store proper payload" in new DatabaseFixture {
        val properPayload = LibraryDTO("vss", "Mike", "Some thing for some things", "micheline", "michelson")
        Post("/library", properPayload) ~> Route.seal(libraryRoute) ~> check {
          status shouldBe StatusCodes.OK
        }

        val dbRequest      = testDb.run(LibraryTable.library.filter(_.name === "vss").result)
        val currentLibrary = Await.result(dbRequest,  1 second)

        currentLibrary.headOption.isEmpty shouldBe false
        val addedRecord = currentLibrary.head
        addedRecord.name        shouldBe "vss"
        addedRecord.author      shouldBe "Mike"
        addedRecord.description shouldBe "Some thing for some things"
        addedRecord.micheline   shouldBe "micheline"
        addedRecord.michelson   shouldBe "michelson"
        addedRecord.status      shouldBe None
      }

      "show only accepted records" in new DatabaseFixture {
        val expectedRecord1 = LibraryDTO("nameE1", "authorE1", "descriptionE1", "michelineE1", "michelsonE1")
        val expectedRecord2 = LibraryDTO("nameE2", "authorE2", "descriptionE2", "michelineE2", "michelsonE2")
        val expectedRecord3 = LibraryDTO("nameE3", "authorE3", "descriptionE3", "michelineE3", "michelsonE3")

        val expectedRecords = List(expectedRecord1, expectedRecord2, expectedRecord3)

        runDB(
          dbAction =
            sqlu"""insert into library (name, author, description, micheline, michelson, status) values
                (${expectedRecord1.name}, ${expectedRecord1.author}, ${expectedRecord1.description}, ${expectedRecord1.micheline}, ${expectedRecord1.michelson}, 1),
                (${expectedRecord2.name}, ${expectedRecord2.author}, ${expectedRecord2.description}, ${expectedRecord2.micheline}, ${expectedRecord2.michelson}, 1),
                (${expectedRecord3.name}, ${expectedRecord3.author}, ${expectedRecord3.description}, ${expectedRecord3.micheline}, ${expectedRecord3.michelson}, 1),
                ('nameNE1', 'authorNE1', 'descriptionNE1', 'michelineNE1', 'michelsonNE1', 0),
                ('nameNE2', 'authorNE2', 'descriptionNE2', 'michelineNE2', 'michelsonNE2', null)"""
        )

        Get("/library") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryDTO]]
          actualRecords should contain theSameElementsAs expectedRecords
        }
      }

      "show records using a limit parameter or using the default limit" in new DatabaseFixture {
        val defaultLimit = config.dbUtility.defaultLimit
        val manualLimit  = 3

        insertDummiesToDb(defaultLimit)

        Get(s"/library?limit=$manualLimit") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryDTO]]
          actualRecords.size shouldBe manualLimit
        }

        Get(s"/library") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryDTO]]
          actualRecords.size shouldBe defaultLimit
        }
      }

    }
}
