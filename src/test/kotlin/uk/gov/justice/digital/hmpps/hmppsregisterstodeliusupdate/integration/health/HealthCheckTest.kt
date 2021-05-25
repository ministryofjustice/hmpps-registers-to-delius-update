package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.integration.health

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.CourtRegisterApiExtension
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.wiremock.ProbationApiExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

@ExtendWith(ProbationApiExtension::class, CourtRegisterApiExtension::class, HmppsAuthApiExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class HealthCheckTest {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Qualifier("awsSqsClient")
  @Autowired
  internal lateinit var awsSqsClient: AmazonSQS

  @Qualifier("awsSqsDlqClient")
  @Autowired
  internal lateinit var awsSqsDlqClient: AmazonSQS

  @Value("\${sqs.queue.name}")
  lateinit var queueName: String

  @Value("\${sqs.dlq.name}")
  lateinit var dlqName: String

  @BeforeEach
  fun purgeQueue() {
    awsSqsClient.purgeQueue(PurgeQueueRequest(queueName.queueUrl()))
    awsSqsDlqClient.purgeQueue(PurgeQueueRequest(dlqName.queueUrl()))
  }

  fun String.queueUrl(): String = awsSqsClient.getQueueUrl(this).queueUrl

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
