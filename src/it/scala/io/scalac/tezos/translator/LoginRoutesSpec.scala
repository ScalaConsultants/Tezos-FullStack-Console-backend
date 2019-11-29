package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ForEachTestContainer, MySQLContainer}
import com.github.t3hnar.bcrypt._
import io.scalac.tezos.translator.model.{UserCredentials, UserModel}
import io.scalac.tezos.translator.repository.UserRepository
import io.scalac.tezos.translator.routes.{JsonHelper, LoginRoutes}
import io.scalac.tezos.translator.schema.UsersTable
import io.scalac.tezos.translator.service.UserService
import org.scalatest.{FlatSpec, Matchers}
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.language.postfixOps

class LoginRoutesSpec extends FlatSpec with Matchers with ScalatestRouteTest with JsonHelper with ForEachTestContainer {

  override lazy val container = MySQLContainer()

  private trait DatabaseFixture extends DbTestBase {
    val testDb: MySQLProfile.backend.Database = DbTestBase.dbFromContainer(container)
    val loginRoute = new LoginRoutes(new UserService(new UserRepository, testDb)).routes

    createTables()
    runDB(
      UsersTable.users += UserModel("asdf", "zxcv".bcrypt)
    )
  }

  "LoginRoute" should "reject wrong credentials" in new DatabaseFixture {
    Post("/login", UserCredentials("asdf", "asdf")) ~> loginRoute ~> check {
      status shouldBe StatusCodes.Forbidden
    }
  }

  it should "accept correct credentials and return token" in new DatabaseFixture {
    Post("/login", UserCredentials("asdf", "zxcv")) ~> loginRoute ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] should not be empty
    }
  }

  it should "handle non-existing username gracefully" in new DatabaseFixture {
    Post("/login", UserCredentials("zxcv", "zxcv")) ~> loginRoute ~> check {
      status shouldBe StatusCodes.Forbidden
    }
  }
}
