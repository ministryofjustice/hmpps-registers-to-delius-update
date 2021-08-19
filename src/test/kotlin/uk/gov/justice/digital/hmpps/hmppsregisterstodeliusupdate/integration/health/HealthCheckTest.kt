package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.CourtRegisterApiExtension.Companion.courtRegisterApi
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.ProbationApiExtension.Companion.probationApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckTest : SqsIntegrationTestBase() {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Test
  fun `Health page reports ok`() {
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health info reports version`() {
    stubPingWithResponse(200)
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
        }
      )
  }

  @Test
  fun `Queue health reports queue details`() {
    stubPingWithResponse(200)
    val exchange = webTestClient.get().uri("/health")
      .exchange()

    exchange.expectStatus().value {
      assertThat(it).withFailMessage {
        val body = exchange.expectBody().returnResult().responseBody
        "Expected OK status but was $it with json ${String(body)}"
      }.isEqualTo(HttpStatus.OK.value())
    }

    exchange.expectBody()
      .jsonPath("components.registers-health.status").isEqualTo("UP")
      .jsonPath("components.registers-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.registers-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.registers-health.details.messagesOnDlq").isEqualTo(0)
      .jsonPath("components.registers-health.details.dlqStatus").isEqualTo("UP")
  }

  @Test
  fun `Health ping page is accessible`() {
    stubPingWithResponse(200)
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {

    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  private fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
    probationApi.stubHealthPing(status)
    courtRegisterApi.stubHealthPing(status)
  }
}
