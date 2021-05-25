package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class CourtRegisterApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val courtRegisterApi = CourtRegisterApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    courtRegisterApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    courtRegisterApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    courtRegisterApi.stop()
  }
}

class CourtRegisterApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 9082
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
}
