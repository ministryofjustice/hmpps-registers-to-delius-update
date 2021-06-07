package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ProbationApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val probationApi = ProbationApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    probationApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    probationApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    probationApi.stop()
  }
}

class ProbationApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 9081
  }

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status)
      )
    )
  }

  private val courtJsonResponse = """
    {
      "courtId": 2500013000,
      "code": "SHFCC",
      "selectable": true,
      "courtName": "Another Sheffield Crown Court",
      "buildingName": "Another Sheffield Crown Court",
      "street": "The Law Courts",
      "locality": "50 West Bar",
      "town": "Sheffield",
      "county": "South Yorkshire",
      "postcode": "S3 8PH",
      "country": "UK",
      "telephoneNumber": "0123 1234567",
      "fax": "0123 1234567",
      "probationArea": {
        "code": "N02",
        "description": "NPS North East"
      },
      "courtTypeId": 314,
      "courtType": {
        "code": "CRN",
        "description": "Crown Court"
      }
    }
  """.trimIndent()

  fun stubCourtPut(courtId: String) {
    stubFor(
      put("/secure/courts/code/$courtId")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(courtJsonResponse)
            .withStatus(200)
        )
    )
  }

  fun stubCourtPost() {
    stubFor(
      post("/secure/courts")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(courtJsonResponse)
            .withStatus(200)
        )
    )
  }

  fun stubCourtGet(courtId: String, response: String = courtJsonResponse) {
    stubFor(
      get("/secure/courts/code/$courtId").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(response)
          .withStatus(200)
      )
    )
  }

  fun stubCourtGetFail(courtId: String, status: Int) {
    stubFor(
      get("/secure/courts/code/$courtId").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(status)
      )
    )
  }
}
