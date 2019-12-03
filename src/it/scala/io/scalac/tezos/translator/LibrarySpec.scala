package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.scalac.tezos.translator.config.{CaptchaConfig, Configuration}
import io.scalac.tezos.translator.model.LibraryEntry._
import io.scalac.tezos.translator.model._
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto
import io.scalac.tezos.translator.repository.{LibraryRepository, UserRepository}
import io.scalac.tezos.translator.routes.dto.LibraryEntryRoutesDto
import io.scalac.tezos.translator.routes.{JsonHelper, LibraryRoutes}
import io.scalac.tezos.translator.schema.LibraryTable
import io.scalac.tezos.translator.service.{LibraryService, UserService}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, BeforeAndAfterEach, Matchers, WordSpec}
import slick.jdbc.PostgresProfile.api._

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
  with BeforeAndAfterEach {

    override def beforeEach(): Unit = DbTestBase.recreateTables()

    val testDb = DbTestBase.db

    val reCaptchaConfig = CaptchaConfig(checkOn = false, "", "", "")
    val config          = Configuration(reCaptcha = reCaptchaConfig)

    val userService = new UserService(new UserRepository, testDb)

    val libraryRepo = new LibraryRepository(config.dbUtility)
    val libraryService = new LibraryService(libraryRepo, testDb)
    val libraryRoute: Route = new LibraryRoutes(libraryService, userService, system.log, config).routes

    def insertDummiesToDb(size: Int, status: Option[Status] = Some(Accepted)): Future[immutable.IndexedSeq[Int]] = {
      val inserts =for {
        i <- 1 to size
        dummyData = LibraryEntry(
          Uid(),
          "name",
          "author",
          Some("email"),
          "description",
          "micheline",
          "michelson",
          status.getOrElse(Status.fromInt(i % 3).toOption.get))
      } yield libraryService.addNew(dummyData)

      Future.sequence(inserts)
    }

    def checkValidationErrorsWithExpected(dto: LibraryEntryRoutesDto, expectedErrors: List[String]): Assertion = {
      Post("/library", dto) ~> Route.seal(libraryRoute) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[Errors].errors should contain theSameElementsAs expectedErrors
      }
    }

    private def getToken(userService: UserService, user: UserCredentials): String = {
      val maybeToken = Await.result(userService.authenticateAndCreateToken(user.username, user.password), 3 seconds)

      maybeToken shouldBe a[Some[_]]

      maybeToken.get
    }

    val longField: String = "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop" +
      "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqw" +
      "ertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop"

    "Library routes" should {
      "validate payload before storing" in {
        val emptyPayload = LibraryEntryRoutesDto("", "", None, "", "", "")
        val expectedErrors1 = List("name field is empty", "author field is empty", "description field is empty",
          "micheline field is empty", "michelson field is empty")

        checkValidationErrorsWithExpected(emptyPayload, expectedErrors1)

        val toLongPayload = LibraryEntryRoutesDto(longField, longField, None, "description", "some", "some")
        val expectedErrors2 = List("field name is too long, max length - 255", "field author is too long, max length - 255")

        checkValidationErrorsWithExpected(toLongPayload, expectedErrors2)
      }

      "store proper payload" in {
        val properPayload = LibraryEntryRoutesDto("vss", "Mike", None, "Some thing for some things", "micheline", "michelson")
        Post("/library", properPayload) ~> Route.seal(libraryRoute) ~> check {
          status shouldBe StatusCodes.OK
        }

        val dbRequest: Future[Seq[LibraryEntryDbDto]] = testDb.run(LibraryTable.library.filter(_.name === "vss").result)
        val currentLibrary: Seq[LibraryEntryDbDto] = Await.result(dbRequest,  1 second)

        currentLibrary.headOption.isEmpty shouldBe false
        val addedRecord: LibraryEntryDbDto = currentLibrary.head
        addedRecord.name        shouldBe "vss"
        addedRecord.author      shouldBe "Mike"
        addedRecord.description shouldBe "Some thing for some things"
        addedRecord.micheline   shouldBe "micheline"
        addedRecord.michelson   shouldBe "michelson"
        addedRecord.status      shouldBe PendingApproval.value
      }

      "correctly filter records" in new SampleEntries {

        whenReady(insert(libraryService)) { _ should contain theSameElementsAs Seq(1, 1, 1) }

        // it was the only one accepted
        val expectedRecord2 = LibraryEntryRoutesDto("nameE2", "authorE2", Some("name@service.com"), "descriptionE2", "michelineE2", "michelsonE2")

        whenReady(libraryService.getRecords()) {
          _ should contain theSameElementsAs toInsert
        }
        whenReady(libraryService.getRecords(statusFilter = Some(Accepted))) {
          _ should contain theSameElementsAs Seq(record2)
        }

        Get("/library") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
          actualRecords should contain theSameElementsAs Seq(expectedRecord2)
        }
      }

      "show records using a limit parameter or using the default limit" in {
        val defaultLimit: Int = config.dbUtility.defaultLimit
        val manualLimit  = 3

        whenReady(insertDummiesToDb(defaultLimit + 1)) {
          _.length shouldBe defaultLimit + 1
        }

        Get(s"/library?limit=$manualLimit") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
          actualRecords.size shouldBe manualLimit
        }

        Get(s"/library") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
          actualRecords.size shouldBe defaultLimit
        }
      }

      "update library entry status" in new SampleEntries {
        whenReady(insert(libraryService)) { _ should contain theSameElementsAs Seq(1, 1, 1) }

        val expectedRecord2 = LibraryEntryRoutesDto("nameE2", "authorE2", Some("name@service.com"), "descriptionE2", "michelineE2", "michelsonE2")

        Get("/library") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
          actualRecords should contain theSameElementsAs Seq(expectedRecord2)
        }

        val bearerToken = getToken(userService, UserCredentials("asdf", "zxcv"))

        // change statuses
        // record1
        Put(s"/library?uid=d7327913-4957-4417-96d2-e5c1d4311f80&status=accepted").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
        }
        // record2
        Put("/library?uid=17976f3a-505b-4d66-854a-243a70bb94c0&status=declined").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
        }
        // record3
        Put("/library?uid=5d8face2-ab24-49e0-b792-a0b99a031645&status=pending_approval").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
        }

        // invalid uid
        Put("/library?uid=aada8ebe&status=pending_approval").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
          status shouldBe StatusCodes.BadRequest
        }
        // non exisitng uid
        Put("/library?uid=4cb9f377-718c-4d5d-be0d-118a5c99e298&status=pending_approval").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
          status shouldBe StatusCodes.Forbidden
        }


        val expectedNewStatuses = Seq(record1.copy(status = Accepted), record2.copy(status = Declined), record3.copy(status = PendingApproval))
        whenReady(libraryService.getRecords(limit = Some(5))) {
          _ should contain theSameElementsAs expectedNewStatuses
        }

        val expectedRecord1 = LibraryEntryRoutesDto("nameE1", "authorE1", None, "descriptionE1", "michelineE1", "michelsonE1")

        Get("/library") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
          actualRecords should contain theSameElementsAs Seq(expectedRecord1)
        }
      }

      "delete entry" in new SampleEntries {
        whenReady(insert(libraryService)) { _ should contain theSameElementsAs Seq(1, 1, 1) }

        val expectedRecord2 = LibraryEntryRoutesDto("nameE2", "authorE2", Some("name@service.com"), "descriptionE2", "michelineE2", "michelsonE2")

        Get("/library") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
          actualRecords should contain theSameElementsAs Seq(expectedRecord2)
        }

        val bearerToken = getToken(userService, UserCredentials("asdf", "zxcv"))

        // record 2
        Delete("/library?uid=17976f3a-505b-4d66-854a-243a70bb94c0").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
        }

        Get("/library") ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
          actualRecords shouldBe 'empty
        }

        // invalid uid
        Delete("/library?uid=aada8ebe").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
          status shouldBe StatusCodes.BadRequest
        }
        // non exisitng uid
        Delete("/library?uid=4cb9f377-718c-4d5d-be0d-118a5c99e298").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
          status shouldBe StatusCodes.Forbidden
        }
      }

      "display full library to admins" in new DatabaseFixture with SampleEntries {

        whenReady(insert(libraryService)) {
          _ should contain theSameElementsAs Seq(1, 1, 1)
        }

        val bearerToken = getToken(userService, UserCredentials("asdf", "zxcv"))
        Get(s"/library").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
          actualRecords.size shouldBe toInsert.length
        }
      }

      "correctly paginate results" in new DatabaseFixture with SampleEntries {

        whenReady(insert(libraryService)) {
          _ should contain theSameElementsAs Seq(1, 1, 1)
        }

        val bearerToken = getToken(userService, UserCredentials("asdf", "zxcv"))
        Get(s"/library").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
          status shouldBe StatusCodes.OK
          val actualAllRecords = responseAs[List[LibraryEntryRoutesDto]]
          actualAllRecords.size shouldBe toInsert.length

          Get(s"/library?limit=2").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
            status shouldBe StatusCodes.OK
            val actualPaginatedRecords = responseAs[List[LibraryEntryRoutesDto]]
            actualPaginatedRecords should contain theSameElementsAs (actualAllRecords.slice(0, 2))

          }

          Get(s"/library?limit=2&offset=2").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
            status shouldBe StatusCodes.OK
            val actualPaginatedRecords = responseAs[List[LibraryEntryRoutesDto]]
            actualPaginatedRecords should contain theSameElementsAs (actualAllRecords.slice(2, 4))
          }
        }
      }

    }

  private trait SampleEntries {
    val record1 = LibraryEntry(Uid.fromString("d7327913-4957-4417-96d2-e5c1d4311f80").get, "nameE1", "authorE1", None, "descriptionE1", "michelineE1", "michelsonE1", PendingApproval)
    val record2 = LibraryEntry(Uid.fromString("17976f3a-505b-4d66-854a-243a70bb94c0").get, "nameE2", "authorE2", Some("name@service.com"), "descriptionE2", "michelineE2", "michelsonE2", Accepted)
    val record3 = LibraryEntry(Uid.fromString("5d8face2-ab24-49e0-b792-a0b99a031645").get, "nameE3", "authorE3", None, "descriptionE3", "michelineE3", "michelsonE3", Declined)

    val toInsert = Seq(record1, record2, record3)

    def insert(libraryService: LibraryService): Future[Seq[Int]] = Future.sequence(toInsert.map(r => libraryService.addNew(r)))
  }
}
