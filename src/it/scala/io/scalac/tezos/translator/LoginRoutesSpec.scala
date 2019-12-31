package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.scalac.tezos.translator.repository.UserRepository
import io.scalac.tezos.translator.routes.LoginRoutes
import io.scalac.tezos.translator.service.UserService
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import Helper._
import eu.timepit.refined.collection.NonEmpty
import io.scalac.tezos.translator.model.UserCredentials
import eu.timepit.refined.refineMV
import io.scalac.tezos.translator.model.types.Auth.{Password, Username, UsernameReq}

import scala.language.postfixOps

class LoginRoutesSpec
  extends FlatSpec
  with Matchers
  with ScalatestRouteTest
  with ScalaFutures
  with BeforeAndAfterEach {
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    import io.circe.generic.auto._

    override def beforeEach(): Unit = DbTestBase.recreateTables()
    val loginEndpoint  = "/v1/login"
    val logoutEndpoint = "/v1/logout"
    val testDb = DbTestBase.db
    val credentialsWithInvalidPassword: UserCredentials = UserCredentials(adminUsername, Password(refineMV[NonEmpty]("asdf")))

    val loginRoute: Route = new LoginRoutes(new UserService(new UserRepository, testDb), system.log).routes

    "LoginRoute" should "reject wrong credentials" in {
      Post(loginEndpoint, UserCredentials(adminUsername, Password(refineMV[NonEmpty]("asdf")))) ~> loginRoute ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }

    it should "accept correct credentials and return token" in {
      Post(loginEndpoint, adminCredentials) ~> loginRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] should not be empty
      }
    }

    it should "handle non-existing username gracefully" in {
      Post(loginEndpoint, UserCredentials(Username(refineMV[UsernameReq]("zxcv")), adminPassword)) ~> loginRoute ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }

    it should "allow to logout" in {
      Post(loginEndpoint, adminCredentials) ~> loginRoute ~> check {
        val token = responseAs[String]
        Post(logoutEndpoint).withHeaders(Authorization(OAuth2BearerToken(token))) ~> loginRoute ~> check {
          status shouldBe StatusCodes.OK
        }
      }
    }

    it should "only allowed logout for users who are logged in" in {
      Post(logoutEndpoint).withHeaders(Authorization(OAuth2BearerToken("someRandomTokenWithLength30Sym"))) ~> Route.seal(loginRoute) ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    it should "not allow to logout twice" in {
      Post(loginEndpoint, adminCredentials) ~> loginRoute ~> check {
        val token = responseAs[String]
        Post(logoutEndpoint).withHeaders(Authorization(OAuth2BearerToken(token))) ~> loginRoute ~> check {
          status shouldBe StatusCodes.OK
        }
        Post(logoutEndpoint).withHeaders(Authorization(OAuth2BearerToken(token))) ~> Route.seal(loginRoute) ~> check {
          status shouldBe StatusCodes.Unauthorized
        }
      }
    }
}
