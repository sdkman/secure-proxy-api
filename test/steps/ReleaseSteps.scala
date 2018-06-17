package steps

import com.github.tomakehurst.wiremock.client.WireMock._
import cucumber.api.scala.{EN, ScalaDsl}
import org.scalatest.Matchers
import play.api.libs.json.Json
import scalaj.http.{Http, HttpOptions}
import support.World
import support.World.appHost

class ReleaseSteps extends ScalaDsl with EN with Matchers {

  When("""the remote release service is unavailable""") { () =>
    //nothing to do
  }

  When("""^posting JSON on the (.*) endpoint:$""") { (endpoint: String, payload: String) =>
    val response = Http(appHost + endpoint)
      .postData(payload.stripMargin)
      .headers(World.headers.toMap)
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(10000))
      .asString
    World.responseCode = response.code
    World.responseBody = response.body
  }

  Then("""^the status received is (.*)$""") { status: String =>
    World.responseCode shouldBe World.statusCodes(status)
  }

  sealed case class ApiResponse(status: Int, id: Option[String], message: String)

  implicit val responseReads = Json.reads[ApiResponse]

  Then("""^the response is:$""") { expectedJson: String =>
    val actual = Json.parse(World.responseBody).as[ApiResponse]
    val expected = Json.parse(expectedJson.stripMargin).as[ApiResponse]
    actual shouldBe expected
  }

  Given("""^the remote release service returns a (.*) response:$""") { (status: String, payload: String) =>
    stubFor(post(urlEqualTo("/release"))
      .willReturn(aResponse()
        .withBody(payload.stripMargin)
        .withStatus(World.statusCodes(status))
      )
    )
  }

  Then("""^the remote release service expects payload and appropriate headers:$""") { payload: String =>
    verify(postRequestedFor(urlEqualTo("/release"))
      .withRequestBody(equalToJson(payload.stripMargin))
      .withHeader("Service-Token", equalTo("default_token"))
      .withHeader("Consumer", equalTo("groovy"))
    )
  }

  And("""^the remote release service expects NO posts$""") { () =>
    verify(0, postRequestedFor(urlEqualTo("/release")))
  }

}
