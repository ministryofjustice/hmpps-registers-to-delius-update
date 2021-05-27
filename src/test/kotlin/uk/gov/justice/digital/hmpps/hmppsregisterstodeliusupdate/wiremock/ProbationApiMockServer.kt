package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
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

  private val courtPutJsonResponse = """
    {
      "courtId": "SHFCC",
      "courtName": "Another Sheffield Crown Court",
      "active": true,
      "courtTypeCode": "CRN",
      "buildingName": "Another Sheffield Crown Court",
      "street": "The Law Courts2",
      "locality": "50 West Bar2",
      "town": "Sheffield2",
      "postcode": "S3 8PH",
      "county": "South Yorkshire2",
      "country": "UK",
      "telephoneNumber": "0114 24565432"
    }
    """.trimIndent()

  fun stubCourtPut(courtId: String) {
    stubFor(
      put("/secure/courts/code/$courtId")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(courtPutJsonResponse)
            .withStatus(200)
        )
    )
  }
}
