package io.scalac.tezos.translator

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.scalac.tezos.translator.config.{CaptchaConfig, DBUtilityConfiguration}
import io.scalac.tezos.translator.model.LibraryEntry._
import io.scalac.tezos.translator.model._
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto
import io.scalac.tezos.translator.repository.{Emails2SendRepository, LibraryRepository, UserRepository}
import io.scalac.tezos.translator.routes.dto.{LibraryEntryRoutesAdminDto, LibraryEntryRoutesDto}
import io.scalac.tezos.translator.routes.dto.DTO.Errors
import io.scalac.tezos.translator.routes.LibraryRoutes
import io.scalac.tezos.translator.schema.LibraryTable
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, UserService}
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
    with BeforeAndAfterEach {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  override def beforeEach(): Unit = DbTestBase.recreateTables()

  val testDb = DbTestBase.db

  val reCaptchaConfig = CaptchaConfig(checkOn = false, "", "", "")
  val dbUtilityConfig = DBUtilityConfiguration()
  val log: LoggingAdapter = system.log

  val userService = new UserService(new UserRepository, testDb)
  val emails2SendRepo = new Emails2SendRepository
  val email2SendService = new Emails2SendService(emails2SendRepo, testDb)
  val libraryRepo = new LibraryRepository(dbUtilityConfig, testDb)
  val libraryService = new LibraryService(libraryRepo, log)
  val adminEmail = EmailAddress.fromString("tezos-console-admin@service.com").get
  val libraryRoute: Route = new LibraryRoutes(libraryService, userService, email2SendService, system.log, reCaptchaConfig, adminEmail).routes

  def insertDummiesToDb(size: Int, status: Option[Status] = Some(Accepted)): Future[immutable.IndexedSeq[Int]] = {
    val inserts = for {
      i <- 1 to size
      dummyData = LibraryEntry(
        Uid(),
        "name",
        Option("ThomasTheTest"),
        EmailAddress.fromString("name@service.com").toOption,
        Option("Testing thing"),
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

    maybeToken shouldBe defined

    maybeToken.get
  }

  val longField: String = "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop" +
    "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqw" +
    "ertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop"

  "Library routes" should {


    "validate payload before storing" in {
      val emptyPayload = LibraryEntryRoutesDto("", Some("asda"), None, Some("asd"), "", "")
      val expectedErrors1 = List("name field is empty",
        "micheline field is empty", "michelson field is empty")

      checkValidationErrorsWithExpected(emptyPayload, expectedErrors1)

      val toLongPayload = LibraryEntryRoutesDto(longField, Some(longField), None, None, "some", "some")
      val expectedErrors2 = List("field name is too long, max length - 255", "field author is too long, max length - 255")

      checkValidationErrorsWithExpected(toLongPayload, expectedErrors2)
    }

    "store proper payload" should {
      "full filed payload " in {
        val properPayload = LibraryEntryRoutesDto("vss", Some("Mike"), None, Some("Some thing for some things"), "micheline", "michelson") // with all of some as none return 500
        Post("/library", properPayload) ~> Route.seal(libraryRoute) ~> check {
          status shouldBe StatusCodes.OK
        }

        val dbRequest: Future[Seq[LibraryEntryDbDto]] = testDb.run(LibraryTable.library.filter(_.name === "vss").result)
        val currentLibrary: Seq[LibraryEntryDbDto] = Await.result(dbRequest, 1 second)

        currentLibrary.headOption.isEmpty shouldBe false
        val addedRecord: LibraryEntryDbDto = currentLibrary.head
        addedRecord.title shouldBe "vss"
        addedRecord.author.get shouldBe "Mike"
        addedRecord.description.get shouldBe "Some thing for some things"
        addedRecord.micheline shouldBe "micheline"
        addedRecord.michelson shouldBe "michelson"
      }
      "payload without options" in {
        val properPayload = LibraryEntryRoutesDto("vss", None, None, None, "micheline", "michelson")
        Post("/library", properPayload) ~> Route.seal(libraryRoute) ~> check {
          status shouldBe StatusCodes.OK
        }
        val dbRequest: Future[Seq[LibraryEntryDbDto]] = testDb.run(LibraryTable.library.filter(_.name === "vss").result)
        val currentLibrary: Seq[LibraryEntryDbDto] = Await.result(dbRequest, 1 second)

        currentLibrary.headOption.isEmpty shouldBe false
        val addedRecord: LibraryEntryDbDto = currentLibrary.head
        addedRecord.title shouldBe "vss"
        addedRecord.micheline shouldBe "micheline"
        addedRecord.michelson shouldBe "michelson"
      }
      "payload with UperCased Email make lower " in {
        val properPayload = LibraryEntryRoutesDto("vss", None, Some("Aeaaast@service.pl"), Some("Some thing for some things"), "micheline", "michelson")
        Post("/library", properPayload) ~> Route.seal(libraryRoute) ~> check {
          status shouldBe StatusCodes.OK
        }
        val dbRequest: Future[Seq[LibraryEntryDbDto]] = testDb.run(LibraryTable.library.filter(_.name === "vss").result)
        val currentLibrary: Seq[LibraryEntryDbDto] = Await.result(dbRequest, 1 second)

        currentLibrary.headOption.isEmpty shouldBe false
        val addedRecord: LibraryEntryDbDto = currentLibrary.head
        addedRecord.title shouldBe "vss"
        addedRecord.email.get shouldBe "aeaaast@service.pl"
        addedRecord.description.get shouldBe "Some thing for some things"
        addedRecord.micheline shouldBe "micheline"
        addedRecord.michelson shouldBe "michelson"
      }
    }
    "add emails to queue when new library entry added and status changed" in {
      whenReady(email2SendService.getEmails2Send(10)) {
        _ shouldBe 'empty
      }
      val userEmail = "name@service.com"
      val record = LibraryEntryRoutesDto("name", Some("Author"), Some(userEmail), Some("description"), "micheline", "michelson")
      Post("/library", record) ~> Route.seal(libraryRoute) ~> check {
        status shouldBe StatusCodes.OK
      }

    }
  }

  "correctly filter records" in new SampleEntries {

    whenReady(insert(libraryService)) {
      _ should contain theSameElementsAs Seq(1, 1, 1)
    }

    // it was the only one accepted
    val expectedRecord2 = LibraryEntryRoutesDto("nameE2", Some("authorE2"), Some("name@service.com"), Some("descriptionE2"), "michelineE2", "michelsonE2")

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
    val defaultLimit: Int = dbUtilityConfig.defaultLimit
    val manualLimit = 3

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
    whenReady(insert(libraryService)) {
      _ should contain theSameElementsAs Seq(1, 1, 1)
    }

    val expectedRecord2 = LibraryEntryRoutesDto("nameE2", Some("authorE2"), Some("name@service.com"), Some("descriptionE2"), "michelineE2", "michelsonE2")

    Get("/library") ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualRecords should contain theSameElementsAs Seq(expectedRecord2)
    }

    val bearerToken = getToken(userService, UserCredentials("admin", "zxcv"))

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
      status shouldBe StatusCodes.NotFound // cannot update status to "pending_approval"
    }

    // invalid uid
    Put("/library?uid=aada8ebe&status=accepted").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.NotFound
    }
    // non exisitng uid
    Put("/library?uid=4cb9f377-718c-4d5d-be0d-118a5c99e298&status=accepted").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.NotFound
    }


    val expectedNewStatuses = Seq(record1.copy(status = Accepted), record2.copy(status = Declined), record3)
    whenReady(libraryService.getRecords(limit = Some(5))) {
      _ should contain theSameElementsAs expectedNewStatuses
    }

    val expectedRecord1 = LibraryEntryRoutesDto("nameE1", Some("authorE1"), None, Some("descriptionE1"), "michelineE1", "michelsonE1")

    Get("/library") ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualRecords should contain theSameElementsAs Seq(expectedRecord1)
    }
  }

  "delete entry" in new SampleEntries {
    whenReady(insert(libraryService)) {
      _ should contain theSameElementsAs Seq(1, 1, 1)
    }

    val expectedRecord2 = LibraryEntryRoutesDto("nameE2", Some("authorE2"), Some("name@service.com"), Some("descriptionE2"), "michelineE2", "michelsonE2")

    Get("/library") ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualRecords should contain theSameElementsAs Seq(expectedRecord2)
    }

    val bearerToken = getToken(userService, UserCredentials("admin", "zxcv"))

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
      status shouldBe StatusCodes.NotFound
    }
    // non exisitng uid
    Delete("/library?uid=4cb9f377-718c-4d5d-be0d-118a5c99e298").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  "display full library to admins" in new SampleEntries {

    whenReady(insert(libraryService)) {
      _ should contain theSameElementsAs Seq(1, 1, 1)
    }

    val expected = toInsert.map(LibraryEntryRoutesAdminDto.fromDomain)

    val bearerToken = getToken(userService, UserCredentials("admin", "zxcv"))
    Get(s"/library").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualRecords = responseAs[List[LibraryEntryRoutesAdminDto]]
      actualRecords.size shouldBe toInsert.length

      actualRecords should contain theSameElementsAs expected
    }
  }

  "correctly paginate results" in new SampleEntries {

    whenReady(insert(libraryService)) {
      _ should contain theSameElementsAs Seq(1, 1, 1)
    }

    val bearerToken = getToken(userService, UserCredentials("admin", "zxcv"))
    Get(s"/library").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualAllRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualAllRecords.size shouldBe toInsert.length

      Get(s"/library?limit=2").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        val actualPaginatedRecords = responseAs[List[LibraryEntryRoutesDto]]
        actualPaginatedRecords should contain theSameElementsAs actualAllRecords.slice(0, 2)

      }

      Get(s"/library?limit=2&offset=2").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        val actualPaginatedRecords = responseAs[List[LibraryEntryRoutesDto]]
        actualPaginatedRecords should contain theSameElementsAs actualAllRecords.slice(2, 4)
      }
    }
  }

  "add emails to queue when new library entry added and status changed" in {
    whenReady(email2SendService.getEmails2Send(10)) {
      _ shouldBe 'empty
    }

    val userEmail = "name@service.com"
    val userDescription = "name@service.com"
    val userName = "name@service.com"
    val record = LibraryEntryRoutesDto("name", Some(userName), Some(userEmail), Some(userDescription), "micheline", "michelson")


    Post("/library", record) ~> Route.seal(libraryRoute) ~> check {
      status shouldBe StatusCodes.OK
    }

    whenReady(email2SendService.getEmails2Send(10)) { emails2send =>
      emails2send.length shouldBe 1

      val approvalRequest = emails2send.head

      approvalRequest.to shouldBe adminEmail
      approvalRequest.subject shouldBe "Library approval request"
      EmailContent.toPrettyString(approvalRequest.content) should contain
      """
        |Please add my translation to your library:
        |Title: name
        |Description: description
          """.stripMargin

      email2SendService.removeSentMessage(approvalRequest.uid)
    }

    val records = Await.result(libraryService.getRecords(), 3 seconds)

    records.length shouldBe 1

    val maybeRecord = records.headOption

    maybeRecord shouldBe defined

    val recordFromDB = maybeRecord.get

    val bearerToken = getToken(userService, UserCredentials("admin", "zxcv"))

    Put(s"/library?uid=${recordFromDB.uid.value}&status=accepted").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
    }

    whenReady(libraryService.getRecords()) { records =>
      val updatedRecord = records.filter(_.uid == recordFromDB.uid)

      updatedRecord.length shouldBe 1
      updatedRecord.headOption shouldBe Some(recordFromDB.copy(status = Accepted))
    }

    whenReady(email2SendService.getEmails2Send(10)) { emails2send =>
      emails2send.length shouldBe 1

      val approvalRequest = emails2send.head

      approvalRequest.to.toString shouldBe userEmail
      approvalRequest.subject shouldBe "Acceptance status of your Translation has changed"
      approvalRequest.content shouldBe TextContent("""Acceptance status of your translation: "name" has changed to: accepted""")

      email2SendService.removeSentMessage(approvalRequest.uid)
    }
  }



  private trait SampleEntries {
    val record1 = LibraryEntry(Uid.fromString("d7327913-4957-4417-96d2-e5c1d4311f80").get, "nameE1", Some(("authorE1")), None, Some(("descriptionE1")), "michelineE1", "michelsonE1", PendingApproval)
    val record2 = LibraryEntry(Uid.fromString("17976f3a-505b-4d66-854a-243a70bb94c0").get, "nameE2", Some(("authorE2")), Some(EmailAddress.fromString("name@service.com").get), Some(("descriptionE2")), "michelineE2", "michelsonE2", Accepted)
    val record3 = LibraryEntry(Uid.fromString("5d8face2-ab24-49e0-b792-a0b99a031645").get, "nameE3", Some(("authorE3")), None, Some(("descriptionE3")), "michelineE3", "michelsonE3", Declined)

    val toInsert = Seq(record1, record2, record3)

    def insert(libraryService: LibraryService): Future[Seq[Int]] = Future.sequence(toInsert.map(r => libraryService.addNew(r)))
  }

}
