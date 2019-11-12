package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.{Errors, LibraryDTO}
import io.scalac.tezos.translator.repository.LibraryRepository
import io.scalac.tezos.translator.routes.{JsonHelper, LibraryRoutes}
import io.scalac.tezos.translator.schema.LibraryTable
import io.scalac.tezos.translator.service.LibraryService
import org.scalatest.{Assertion, BeforeAndAfterAll, Matchers, WordSpec}
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class LibrarySpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll with JsonHelper with DbTestBase {

  val reCaptchaConfig = CaptchaConfig(checkOn = false, "", "", "")
  val testDb: MySQLProfile.backend.Database = Database.forConfig("tezos-db")
  val libraryRepo = new LibraryRepository
  val libraryService = new LibraryService(libraryRepo, testDb)
  val libraryRoute: Route = new LibraryRoutes(libraryService, system.log, reCaptchaConfig).routes
  val longField: String = "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop" +
    "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqw" +
    "ertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiop"

  override def beforeAll(): Unit = {
    recreateTables()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    testDb.close()
  }

  "Library routes" should {
    "validate payload before storing" in {
      val emptyPayload = LibraryDTO("", "", "", "", "")
      val expectedErrors1 = List("name field is empty", "author field is empty", "description field is empty",
        "micheline field is empty", "michelson field is empty")

      checkValidationErrorsWithExpected(emptyPayload, expectedErrors1)

      val toLongPayload = LibraryDTO(longField, longField, "description", "some", "some")
      val expectedErrors2 = List("field name is too long, max length - 255", "field author is too long, max length - 255")

      checkValidationErrorsWithExpected(toLongPayload, expectedErrors2)
    }

    "store proper payload" in {
      val properPayload = LibraryDTO("vss", "Mike", "Some thing for some things", "micheline", "michelson")
      Post("/library", properPayload) ~> Route.seal(libraryRoute) ~> check {
        status shouldBe StatusCodes.OK
      }

      val dbRequest      = testDb.run(LibraryTable.library.result)
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

  }

  def checkValidationErrorsWithExpected(dto: LibraryDTO, expectedErrors: List[String]): Assertion = {
    Post("/library", dto) ~> Route.seal(libraryRoute) ~> check {
      status shouldBe StatusCodes.BadRequest
      responseAs[Errors].errors should contain theSameElementsAs expectedErrors
    }
  }

}
