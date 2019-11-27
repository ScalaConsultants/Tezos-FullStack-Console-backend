package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ForEachTestContainer, MySQLContainer}
import io.scalac.tezos.translator.config.{CaptchaConfig, Configuration}
import io.scalac.tezos.translator.model.LibraryEntry._
import io.scalac.tezos.translator.model._
import io.scalac.tezos.translator.repository.LibraryRepository
import io.scalac.tezos.translator.routes.{JsonHelper, LibraryRoutes}
import io.scalac.tezos.translator.schema.LibraryTable
import io.scalac.tezos.translator.service.LibraryService
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, Matchers, WordSpec}
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

//noinspection TypeAnnotation
class LibrarySpec
  extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with ScalaFutures
  with JsonHelper
  with ForEachTestContainer {
    override lazy val container = MySQLContainer(mysqlImageVersion = DbTestBase.mySqlVersion)

    private trait DatabaseFixture extends DbTestBase {
      val testDb: MySQLProfile.backend.Database = DbTestBase.dbFromContainer(container)

      val libraryService = new LibraryService(libraryRepo, testDb)
      val libraryRoute: Route = new LibraryRoutes(libraryService, system.log, config).routes

      recreateTables()

      def insertDummiesToDb(size: Int): Future[immutable.IndexedSeq[Int]] = {
        val inserts =for {
          _ <- 1 to size
          dummyData = LibraryEntry(Uid(), "name", "author", Some("email"), "description", "micheline", "michelson", Accepted)
        } yield libraryService.addNew(dummyData)

        Future.sequence(inserts)
      }


      def checkValidationErrorsWithExpected(dto: LibraryJsonDTO, expectedErrors: List[String]): Assertion = {
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
        val emptyPayload = LibraryJsonDTO("", "", None, "", "", "")
        val expectedErrors1 = List("name field is empty", "author field is empty", "description field is empty",
          "micheline field is empty", "michelson field is empty")

        checkValidationErrorsWithExpected(emptyPayload, expectedErrors1)

        val toLongPayload = LibraryJsonDTO(longField, longField, None, "description", "some", "some")
        val expectedErrors2 = List("field name is too long, max length - 255", "field author is too long, max length - 255")

        checkValidationErrorsWithExpected(toLongPayload, expectedErrors2)
      }

      "store proper payload" in new DatabaseFixture {
        val properPayload = LibraryJsonDTO("vss", "Mike", None, "Some thing for some things", "micheline", "michelson")
        Post("/library", properPayload) ~> Route.seal(libraryRoute) ~> check {
          status shouldBe StatusCodes.OK
        }

        val dbRequest: Future[Seq[LibraryDbDTO]] = testDb.run(LibraryTable.library.filter(_.name === "vss").result)
        val currentLibrary: Seq[LibraryDbDTO] = Await.result(dbRequest,  1 second)

        currentLibrary.headOption.isEmpty shouldBe false
        val addedRecord: model.LibraryDbDTO = currentLibrary.head
        addedRecord.name        shouldBe "vss"
        addedRecord.author      shouldBe "Mike"
        addedRecord.description shouldBe "Some thing for some things"
        addedRecord.micheline   shouldBe "micheline"
        addedRecord.michelson   shouldBe "michelson"
        addedRecord.status      shouldBe PendingApproval.value
      }

      "show only accepted records" in new DatabaseFixture {
        val record1 = LibraryEntry(Uid(), "nameE1", "authorE1", None, "descriptionE1", "michelineE1", "michelsonE1", PendingApproval)
        val record2 = LibraryEntry(Uid(), "nameE2", "authorE2", Some("name@service.com"), "descriptionE2", "michelineE2", "michelsonE2", Accepted)
        val record3 = LibraryEntry(Uid(), "nameE3", "authorE3", None, "descriptionE4", "michelineE4", "michelsonE4", Declined)

        val toInsert = Seq(record1, record2, record3)

        val inserts = Future.sequence(toInsert.map(r => libraryService.addNew(r)))

        whenReady(inserts) { _ should contain theSameElementsAs Seq(1, 1, 1) }

        // it was the only one accepted
        val expectedRecord2 = LibraryJsonDTO("nameE2", "authorE2", Some("name@service.com"), "descriptionE2", "michelineE2", "michelsonE2")

        whenReady(libraryService.getAll(5)) { _ should contain theSameElementsAs toInsert }
        whenReady(libraryService.getAccepted(5)) { _ should contain theSameElementsAs Seq(record2) }

        Get("/library") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryJsonDTO]]
          actualRecords should contain theSameElementsAs Seq(expectedRecord2)
        }
      }

      "show records using a limit parameter or using the default limit" in new DatabaseFixture {
        val defaultLimit: Int = config.dbUtility.defaultLimit
        val manualLimit  = 3

        whenReady(insertDummiesToDb(defaultLimit)){ _.length shouldBe defaultLimit }

        Get(s"/library?limit=$manualLimit") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryJsonDTO]]
          actualRecords.size shouldBe manualLimit
        }

        Get(s"/library") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryJsonDTO]]
          actualRecords.size shouldBe defaultLimit
        }
      }

    }
}
