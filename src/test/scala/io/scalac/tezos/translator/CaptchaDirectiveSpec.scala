package io.scalac.tezos.translator

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{post => expectedPost, _}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.routes.directives.ReCaptchaDirective
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.duration._

class CaptchaDirectiveSpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll with Directives {

  val checkCaptchaUri     = "/check"
  val testCaptchaHost     = "localhost"
  val testCaptchaPort     = 3030
  val testCaptchaHostName = s"$testCaptchaHost:$testCaptchaPort"
  val secret              = "test"
  val headerName          = "CAPTCHA"

  lazy val wireMockServer = new WireMockServer(wireMockConfig().port(testCaptchaPort))

  implicit def default: RouteTestTimeout = RouteTestTimeout(5.seconds)

  override def beforeAll: Unit = {
    wireMockServer.start()
    WireMock.configureFor(testCaptchaHost, testCaptchaPort)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

  val captchaTestConfig =
    CaptchaConfig(checkOn = true, url = "http://" + testCaptchaHostName + checkCaptchaUri, secret = secret, headerName = headerName)

  val captchaTestRoute: Route = path("test") {
    pathEndOrSingleSlash {
      ReCaptchaDirective.withReCaptchaVerify(system.log, captchaTestConfig)(system) {
        get {
          complete("get ok")
        }
      }
    }
  }

  "ReCaptcha directive" should {

    "check captcha header" in {
      Get("/test") ~> Route.seal(captchaTestRoute) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] shouldBe s"Request is missing required HTTP header '$headerName'"
      }

      val testResponse = "testResponse"
      stubForCaptchaCheck(testResponse)

      Get("/test") ~> addHeader(headerName, testResponse) ~> Route.seal(captchaTestRoute) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "get ok"
      }
    }

    "return 401 status code if captcha is invalid" in {

      val invalidResponse = "invalid"

      stubForInvalidCaptcha(invalidResponse)

      Get("/test") ~> addHeader(headerName, invalidResponse) ~> Route.seal(captchaTestRoute) ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[String] shouldBe """{"error":"Invalid captcha"}"""
      }

    }

    "not check captcha if checkOn flag is false" in {
      val configWithFalseFlag = captchaTestConfig.copy(checkOn = false)
      val captchaNotCheckRoute: Route = path("test") {
        pathEndOrSingleSlash {
          ReCaptchaDirective.withReCaptchaVerify(system.log, configWithFalseFlag)(system) {
            get {
              complete("get ok")
            }
          }
        }
      }

      Get("/test") ~> captchaNotCheckRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "get ok"
      }
    }

  }

  def stubForCaptchaCheck(expectedUserResponse: String): Unit =
    wireMockServer.stubFor(
       expectedPost(urlEqualTo(checkCaptchaUri + s"?secret=$secret&response=$expectedUserResponse"))
         .willReturn(
            aResponse()
              .withHeader("Content-Type", "application/json")
              .withBody("""{
                |"success": true
                |}""".stripMargin)
              .withStatus(StatusCodes.OK.intValue)
         )
    )

  def stubForInvalidCaptcha(expectedUserResponse: String): Unit =
    wireMockServer.stubFor(
       expectedPost(urlEqualTo(checkCaptchaUri + s"?secret=$secret&response=$expectedUserResponse"))
         .willReturn(
            aResponse()
              .withHeader("Content-Type", "application/json")
              .withBody("""{
                |"success": false,
                |"error-codes": [
                |"invalid-input-response"
                |]
                |}""".stripMargin)
              .withStatus(StatusCodes.OK.intValue)
         )
    )

}
