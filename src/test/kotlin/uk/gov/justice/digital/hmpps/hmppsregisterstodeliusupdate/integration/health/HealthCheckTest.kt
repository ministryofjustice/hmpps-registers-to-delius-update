package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.CourtRegisterApiExtension
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.ProbationApiExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckTest : IntegrationTestBase() {

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
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.hmppsDomainQueueHealth.details.MessagesOnQueue").isEqualTo(0)
      .jsonPath("components.hmppsDomainQueueHealth.details.MessagesInFlight").isEqualTo(0)
      .jsonPath("components.hmppsDomainQueueHealth.details.MessagesOnDLQ").isEqualTo(0)
      .jsonPath("components.hmppsDomainQueueHealth.details.dlqStatus").isEqualTo("UP")
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
    HmppsAuthApiExtension.hmppsAuth.stubHealthPing(status)
    ProbationApiExtension.probationApi.stubHealthPing(status)
    CourtRegisterApiExtension.courtRegisterApi.stubHealthPing(status)
  }
}
