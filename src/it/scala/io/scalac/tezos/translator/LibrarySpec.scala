package io.scalac.tezos.translator

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import cats.syntax.option._
import eu.timepit.refined._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.string.Uuid
import io.scalac.tezos.translator.Helper.adminCredentials
import io.scalac.tezos.translator.config.{CaptchaConfig, DBUtilityConfiguration}
import io.scalac.tezos.translator.model.LibraryEntry._
import io.scalac.tezos.translator.model._
import io.scalac.tezos.translator.model.types.Auth
import io.scalac.tezos.translator.model.types.ContactData.{EmailReq, EmailS}
import io.scalac.tezos.translator.model.types.Library._
import io.scalac.tezos.translator.model.types.Params.Limit
import io.scalac.tezos.translator.model.types.UUIDs._
import io.scalac.tezos.translator.repository.dto.LibraryEntryDbDto
import io.scalac.tezos.translator.repository.{Emails2SendRepository, LibraryRepository, UserRepository}
import io.scalac.tezos.translator.routes.LibraryRoutes
import io.scalac.tezos.translator.routes.dto.DTO.Errors
import io.scalac.tezos.translator.routes.dto.{LibraryEntryRoutesAdminDto, LibraryEntryRoutesDto}
import io.scalac.tezos.translator.schema.LibraryTable
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, UserService}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, BeforeAndAfterEach, Matchers, WordSpec}
import scalacache._
import scalacache.caffeine._
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

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(5 seconds)
  implicit val timeout = RouteTestTimeout(5.seconds.dilated)

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

  override def beforeEach(): Unit = DbTestBase.recreateTables()

  val libraryEndpoint = "/v1/library"
  val testDb = DbTestBase.db

  val reCaptchaConfig = CaptchaConfig(checkOn = false, "", "", "")
  val dbUtilityConfig = DBUtilityConfiguration()
  val log: LoggingAdapter = system.log

  val properTitle  = Title(refineMV[NotEmptyAndNotLong]("name"))
  val properAuthor = Author(refineMV[NotEmptyAndNotLong]("ThomasTheTest")).some
  val properEmail  = EmailAddress.fromString("name@service.com").toOption
  val properDescription = Description(refineMV[NotEmptyAndNotLong]("Testing thing")).some
  val properMicheline   = Micheline(refineMV[NonEmpty]("micheline"))
  val properMichelson   = Michelson(refineMV[NonEmpty]("michelson"))

  val tokenToUser: Cache[Auth.Username] = CaffeineCache[Auth.Username]
  val userService = new UserService(new UserRepository, tokenToUser, testDb)
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
        generateLibraryEntryId,
        properTitle,
        properAuthor,
        properEmail,
        properDescription,
        properMicheline,
        properMichelson,
        status.getOrElse(Status.fromInt(i % 3).toOption.get))
    } yield libraryService.addNew(dummyData)

    Future.sequence(inserts)
  }

  def checkValidationErrorsWithExpected(dto: LibraryEntryRoutesDto, expectedErrors: List[String]): Assertion = {
    Post(libraryEndpoint, dto) ~> Route.seal(libraryRoute) ~> check {
      status shouldBe StatusCodes.BadRequest
      responseAs[Errors].errors should contain theSameElementsAs expectedErrors
    }
  }

  private def getToken(userService: UserService, user: UserCredentials): String = {
    val maybeToken = Await.result(userService.authenticateAndCreateToken(user.username, user.password), 3 seconds)

    maybeToken shouldBe defined

    maybeToken.get.v.value
  }

  val longField: String = "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop" +
    "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqw" +
    "ertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop"

  "Library routes" should {

    "store proper payload" should {
      "full filed payload " in {
        val titleForFilter  = Title(refineMV[NotEmptyAndNotLong]("vss"))
        val testAuthor      = Author(refineMV[NotEmptyAndNotLong]("Mike")).some
        val testDescription = Description(refineMV[NotEmptyAndNotLong]("Some thing for some things")).some

        val properPayload = LibraryEntryRoutesDto(titleForFilter, testAuthor, None, testDescription, properMicheline, properMichelson) // with all of some as none return 500
        Post(libraryEndpoint, properPayload) ~> Route.seal(libraryRoute) ~> check {
          status shouldBe StatusCodes.OK
        }

        val dbRequest: Future[Seq[LibraryEntryDbDto]] = testDb.run(LibraryTable.library.filter(_.title === titleForFilter).result)
        val currentLibrary: Seq[LibraryEntryDbDto] = Await.result(dbRequest, 1 second)

        currentLibrary.headOption.isEmpty shouldBe false
        val addedRecord: LibraryEntryDbDto = currentLibrary.head
        addedRecord.title.toString shouldBe "vss"
        addedRecord.author.get.toString shouldBe "Mike"
        addedRecord.description.get.toString shouldBe "Some thing for some things"
        addedRecord.micheline.toString shouldBe "micheline"
        addedRecord.michelson.toString shouldBe "michelson"
      }
      "payload without options" in {
        val titleForFilter = Title(refineMV[NotEmptyAndNotLong]("vss"))
        val properPayload  = LibraryEntryRoutesDto(titleForFilter, None, None, None, properMicheline, properMichelson)
        Post(libraryEndpoint, properPayload) ~> Route.seal(libraryRoute) ~> check {
          status shouldBe StatusCodes.OK
        }
        val dbRequest: Future[Seq[LibraryEntryDbDto]] = testDb.run(LibraryTable.library.filter(_.title === titleForFilter).result)
        val currentLibrary: Seq[LibraryEntryDbDto] = Await.result(dbRequest, 1 second)

        currentLibrary.headOption.isEmpty shouldBe false
        val addedRecord: LibraryEntryDbDto = currentLibrary.head
        addedRecord.title.toString shouldBe "vss"
        addedRecord.micheline.toString shouldBe "micheline"
        addedRecord.michelson.toString shouldBe "michelson"
      }
      "payload with UpperCased Email make lower " in {
        val titleForFilter  = Title(refineMV[NotEmptyAndNotLong]("vss"))
        val testDescription = Description(refineMV[NotEmptyAndNotLong]("Some thing for some things")).some
        val properPayload   = LibraryEntryRoutesDto(titleForFilter, None, Some(EmailS(refineMV[EmailReq]("Aeaaast@service.pl"))), testDescription, properMicheline, properMichelson)
        Post(libraryEndpoint, properPayload) ~> Route.seal(libraryRoute) ~> check {
          status shouldBe StatusCodes.OK
        }
        val dbRequest: Future[Seq[LibraryEntryDbDto]] = testDb.run(LibraryTable.library.filter(_.title === titleForFilter).result)
        val currentLibrary: Seq[LibraryEntryDbDto] = Await.result(dbRequest, 1 second)

        currentLibrary.headOption.isEmpty shouldBe false
        val addedRecord: LibraryEntryDbDto = currentLibrary.head
        addedRecord.title.toString shouldBe "vss"
        addedRecord.email.get.toString shouldBe "aeaaast@service.pl"
        addedRecord.description.get.toString shouldBe "Some thing for some things"
        addedRecord.micheline.toString shouldBe "micheline"
        addedRecord.michelson.toString shouldBe "michelson"
      }
    }
    "add emails to queue when new library entry added and status changed" in {
      whenReady(email2SendService.getEmails2Send(10)) {
        _ shouldBe 'empty
      }
      val userEmail = EmailS(refineMV[EmailReq]("name@service.com"))
      val record = LibraryEntryRoutesDto(
        Title(refineMV[NotEmptyAndNotLong]("name")),
        Author(refineMV[NotEmptyAndNotLong]("Author")).some,
        Some(userEmail),
        Description(refineMV[NotEmptyAndNotLong]("description")).some,
        properMicheline,
        properMichelson)
      Post(libraryEndpoint, record) ~> Route.seal(libraryRoute) ~> check {
        status shouldBe StatusCodes.OK
      }

    }
  }

  "correctly filter records" in new SampleEntries {

    whenReady(insert(libraryService)) {
      _ should contain theSameElementsAs Seq(1, 1, 1)
    }

    // it was the only one accepted
    val expectedRecord2 = LibraryEntryRoutesDto(
      Title(refineMV[NotEmptyAndNotLong]("nameE2")),
      Author(refineMV[NotEmptyAndNotLong]("authorE2")).some,
      Some(EmailS(refineMV[EmailReq]("name@service.com"))),
      Description(refineMV[NotEmptyAndNotLong]("descriptionE2")).some,
      Micheline(refineMV[NonEmpty]("michelineE2")),
      Michelson(refineMV[NonEmpty]("michelsonE2")))

    whenReady(libraryService.getRecords()) {
      _ should contain theSameElementsAs toInsert
    }
    whenReady(libraryService.getRecords(statusFilter = Some(Accepted))) {
      _ should contain theSameElementsAs Seq(record2)
    }

    Get(libraryEndpoint) ~> libraryRoute ~> check {
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

    Get(s"$libraryEndpoint?limit=$manualLimit") ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualRecords.size shouldBe manualLimit
    }

    Get(libraryEndpoint) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualRecords.size shouldBe defaultLimit
    }
  }

  "update library entry status" in new SampleEntries {
    whenReady(insert(libraryService)) {
      _ should contain theSameElementsAs Seq(1, 1, 1)
    }

    val expectedRecord2 = LibraryEntryRoutesDto(
      Title(refineMV[NotEmptyAndNotLong]("nameE2")),
      Author(refineMV[NotEmptyAndNotLong]("authorE2")).some,
      Some(EmailS(refineMV[EmailReq]("name@service.com"))),
      Description(refineMV[NotEmptyAndNotLong]("descriptionE2")).some,
      Micheline(refineMV[NonEmpty]("michelineE2")),
      Michelson(refineMV[NonEmpty]("michelsonE2")))

    Get(libraryEndpoint) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualRecords should contain theSameElementsAs Seq(expectedRecord2)
    }

    val bearerToken = getToken(userService, adminCredentials)

    // change statuses
    // record1
    Put(s"$libraryEndpoint?uid=d7327913-4957-4417-96d2-e5c1d4311f80&status=accepted").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
    }
    // record2
    Put(s"$libraryEndpoint?uid=17976f3a-505b-4d66-854a-243a70bb94c0&status=declined").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
    }
    // record3
    Put(s"$libraryEndpoint?uid=5d8face2-ab24-49e0-b792-a0b99a031645&status=pending_approval").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.NotFound // cannot update status to "pending_approval"
    }

    // invalid uid
    Put(s"$libraryEndpoint?uid=aada8ebe&status=accepted").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.BadRequest
    }
    // non exisitng uid
    Put(s"$libraryEndpoint?uid=4cb9f377-718c-4d5d-be0d-118a5c99e298&status=accepted").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.NotFound
    }


    val expectedNewStatuses = Seq(record1.copy(status = Accepted), record2.copy(status = Declined), record3)
    whenReady(libraryService.getRecords(limit = Some(Limit(refineMV[Positive](5))))) {
      _ should contain theSameElementsAs expectedNewStatuses
    }

    val expectedRecord1 = LibraryEntryRoutesDto(
      Title(refineMV[NotEmptyAndNotLong]("nameE1")),
      Author(refineMV[NotEmptyAndNotLong]("authorE1")).some,
      None,
      Description(refineMV[NotEmptyAndNotLong]("descriptionE1")).some,
      Micheline(refineMV[NonEmpty]("michelineE1")),
      Michelson(refineMV[NonEmpty]("michelsonE1")))

    Get(libraryEndpoint) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualRecords should contain theSameElementsAs Seq(expectedRecord1)
    }
  }

  "delete entry" in new SampleEntries {
    whenReady(insert(libraryService)) {
      _ should contain theSameElementsAs Seq(1, 1, 1)
    }

    val expectedRecord2 = LibraryEntryRoutesDto(
      Title(refineMV[NotEmptyAndNotLong]("nameE2")),
      Author(refineMV[NotEmptyAndNotLong]("authorE2")).some,
      Some(EmailS(refineMV[EmailReq]("name@service.com"))),
      Description(refineMV[NotEmptyAndNotLong]("descriptionE2")).some,
      Micheline(refineMV[NonEmpty]("michelineE2")),
      Michelson(refineMV[NonEmpty]("michelsonE2")))

    Get(libraryEndpoint) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualRecords should contain theSameElementsAs Seq(expectedRecord2)
    }

    val bearerToken = getToken(userService, adminCredentials)

    // record 2
    Delete(s"$libraryEndpoint?uid=17976f3a-505b-4d66-854a-243a70bb94c0").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
    }

    Get(libraryEndpoint) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualRecords shouldBe 'empty
    }

    // invalid uid
    Delete(s"$libraryEndpoint?uid=4cb9f377-718c-4d5d-be0d-118a5c99e294").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.NotFound
    }
    // non exisitng uid
    Delete(s"$libraryEndpoint?uid=4cb9f377-718c-4d5d-be0d-118a5c99e298").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  "display full library to admins" in new SampleEntries {

    whenReady(insert(libraryService)) {
      _ should contain theSameElementsAs Seq(1, 1, 1)
    }

    val expected = toInsert.map(LibraryEntryRoutesAdminDto.fromDomain)

    val bearerToken = getToken(userService, adminCredentials)
    Get(libraryEndpoint).withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
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

    val bearerToken = getToken(userService, adminCredentials)
    Get(libraryEndpoint).withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
      status shouldBe StatusCodes.OK
      val actualAllRecords = responseAs[List[LibraryEntryRoutesDto]]
      actualAllRecords.size shouldBe toInsert.length

      Get(s"$libraryEndpoint?limit=2").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        val actualPaginatedRecords = responseAs[List[LibraryEntryRoutesDto]]
        actualPaginatedRecords should contain theSameElementsAs actualAllRecords.slice(0, 2)

      }

      Get(s"$libraryEndpoint?limit=2&offset=2").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
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

    val userEmail = EmailS(refineMV[EmailReq]("name@service.com"))
    val userDescription = Description(refineMV[NotEmptyAndNotLong]("name@service.com"))
    val userName = Author(refineMV[NotEmptyAndNotLong]("name@service.com"))
    val record = LibraryEntryRoutesDto(
      Title(refineMV[NotEmptyAndNotLong]("name")),
      Some(userName),
      Some(userEmail),
      Some(userDescription),
      Micheline(refineMV[NonEmpty]("micheline")),
      Michelson(refineMV[NonEmpty]("michelson")))


    Post(libraryEndpoint, record) ~> Route.seal(libraryRoute) ~> check {
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

    val bearerToken = getToken(userService, adminCredentials)

    Put(s"$libraryEndpoint?uid=${recordFromDB.uid}&status=accepted").withHeaders(Authorization(OAuth2BearerToken(bearerToken))) ~> libraryRoute ~> check {
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

      approvalRequest.to.toString shouldBe userEmail.toString
      approvalRequest.subject shouldBe "Acceptance status of your Translation has changed"
      approvalRequest.content shouldBe TextContent("""Acceptance status of your translation: "name" has changed to: accepted""")

      email2SendService.removeSentMessage(approvalRequest.uid)
    }
  }



  private trait SampleEntries {
    val record1 = LibraryEntry(
      LibraryEntryId(refineMV[Uuid]("d7327913-4957-4417-96d2-e5c1d4311f80")),
      Title(refineMV[NotEmptyAndNotLong]("nameE1")),
      Author(refineMV[NotEmptyAndNotLong]("authorE1")).some,
      None,
      Description(refineMV[NotEmptyAndNotLong]("descriptionE1")).some,
      Micheline(refineMV[NonEmpty]("michelineE1")),
      Michelson(refineMV[NonEmpty]("michelsonE1")),
      PendingApproval)
    val record2 = LibraryEntry(
      LibraryEntryId(refineMV[Uuid]("17976f3a-505b-4d66-854a-243a70bb94c0")),
      Title(refineMV[NotEmptyAndNotLong]("nameE2")),
      Author(refineMV[NotEmptyAndNotLong]("authorE2")).some,
      Some(EmailAddress.fromString("name@service.com").get),
      Description(refineMV[NotEmptyAndNotLong]("descriptionE2")).some,
      Micheline(refineMV[NonEmpty]("michelineE2")),
      Michelson(refineMV[NonEmpty]("michelsonE2")),
      Accepted)
    val record3 = LibraryEntry(
      LibraryEntryId(refineMV[Uuid]("5d8face2-ab24-49e0-b792-a0b99a031645")),
      Title(refineMV[NotEmptyAndNotLong]("nameE3")),
      Author(refineMV[NotEmptyAndNotLong]("authorE3")).some,
      None,
      Description(refineMV[NotEmptyAndNotLong]("descriptionE3")).some,
      Micheline(refineMV[NonEmpty]("michelineE3")),
      Michelson(refineMV[NonEmpty]("michelsonE3")),
      Declined)

    val toInsert = Seq(record1, record2, record3)

    def insert(libraryService: LibraryService): Future[Seq[Int]] = Future.sequence(toInsert.map(r => libraryService.addNew(r)))
  }

}
