package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import cats.syntax.either._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{post => expectedPost, _}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.types.Auth
import io.scalac.tezos.translator.routes.Endpoints
import io.scalac.tezos.translator.routes.Endpoints.ErrorResponse
import io.scalac.tezos.translator.routes.dto.DTO
import io.scalac.tezos.translator.routes.dto.DTO.Error
import io.scalac.tezos.translator.routes.utils.ReCaptcha
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp._

import scala.concurrent.Future
import scala.concurrent.duration._

class CaptchaDirectiveSpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll with Directives {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

  val checkCaptchaUri     = "/check"
  val testCaptchaHost     = "localhost"
  val testCaptchaPort     = 3030
  val testCaptchaHostName = s"$testCaptchaHost:$testCaptchaPort"
  val secret              = "test"
  val headerName          = "CAPTCHA"
  val testEndpoint        = "/v1/test"

  lazy val wireMockServer = new WireMockServer(wireMockConfig().port(testCaptchaPort))

  implicit val sy: ActorSystem      = system

  implicit def default: RouteTestTimeout = RouteTestTimeout(5.seconds)

  override def beforeAll: Unit = {
    wireMockServer.start()
    WireMock.configureFor(testCaptchaHost, testCaptchaPort)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

  val captchaTestConfig: CaptchaConfig = CaptchaConfig(
    checkOn    = true,
    url        = "http://" + testCaptchaHostName + checkCaptchaUri,
    secret     = secret,
    score      = 0.0f,
    headerName = headerName
  )

  def emptyOk(): Future[Either[ErrorResponse, String]] =
    Future.successful("get ok".asRight)

  val captchaTestRoute: Route = Endpoints
    .captchaEndpoint(captchaTestConfig)
    .in("test")
    .get
    .out(jsonBody[String])
    .toRoute((ReCaptcha.withReCaptchaVerify(_, sy.log, captchaTestConfig)).andThenFirstE{ _: Unit => emptyOk() })

  "ReCaptcha directive" should {

    "check captcha header" in {
        val configWithScore= captchaTestConfig.copy(score=0.9F)
        val captchaTestEndpoint: Endpoint[Option[Auth.Captcha], (DTO.ErrorDTO, StatusCode), String, Nothing] = Endpoints
          .captchaEndpoint(configWithScore)
          .in("test")
          .get
          .out(jsonBody[String])

      val captchaTestRoute: Route = captchaTestEndpoint
        .toRoute((ReCaptcha.withReCaptchaVerify(_, sy.log, captchaTestConfig)).andThenFirstE{ _: Unit => emptyOk() })

      Get(testEndpoint) ~> Route.seal(captchaTestRoute) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[Error].error shouldBe s"Request is missing required HTTP header '$headerName'"
      }

      val testResponse = "testResponse"
      stubForCaptchaCheck(testResponse, 1.0F)

      Get(testEndpoint) ~> addHeader(headerName, testResponse) ~> Route.seal(captchaTestRoute) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "get ok"
      }
    }

    "return Bot detected message when score is to low " in {

      val testResponse = "testResponse"

      stubForCaptchaCheck(testResponse, 0.1F)

      Get(testEndpoint) ~> addHeader(headerName, testResponse) ~> Route.seal(captchaTestRoute) ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[Error].error shouldBe "You are bot"
      }

    }
    "return 401 status code if captcha is invalid" in {

      val invalidResponse = "invalid"

      stubForInvalidCaptcha(invalidResponse)

      Get(testEndpoint) ~> addHeader(headerName, invalidResponse) ~> Route.seal(captchaTestRoute) ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[Error].error shouldBe "Invalid captcha"
      }

    }

    "not check captcha if checkOn flag is false" in {
      val configWithFalseFlag = captchaTestConfig.copy(checkOn = false)
      val captchaNotCheckRoute: Route = Endpoints
        .captchaEndpoint(configWithFalseFlag)
        .in("test")
        .get
        .out(jsonBody[String])
        .toRoute((ReCaptcha.withReCaptchaVerify(_, sy.log, configWithFalseFlag)).andThenFirstE{ _: Unit => emptyOk() })

      Get(testEndpoint) ~> captchaNotCheckRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "get ok"
      }
    }
  }

  def stubForCaptchaCheck(expectedUserResponse: String, score:Float=0.0F): Unit =
    wireMockServer.stubFor(
      expectedPost(urlEqualTo(checkCaptchaUri + s"?secret=$secret&response=$expectedUserResponse"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              s"""{
                |"success": true,
                |"score": ${score}
                |}""".stripMargin
            ).withStatus(StatusCodes.OK.intValue)
        )
    )

  def stubForInvalidCaptcha(expectedUserResponse: String): Unit =
    wireMockServer.stubFor(
      expectedPost(urlEqualTo(checkCaptchaUri + s"?secret=$secret&response=$expectedUserResponse"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{
                |"success": false,
                |"error-codes": [
                |"invalid-input-response"
                |]
                |}""".stripMargin
            ).withStatus(StatusCodes.OK.intValue))
    )

}
